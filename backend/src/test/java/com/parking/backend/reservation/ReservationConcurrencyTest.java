package com.parking.backend.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.parking.backend.common.ConflictException;
import com.parking.backend.gate.DetectionRequest;
import com.parking.backend.gate.GateDecision;
import com.parking.backend.gate.GateService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.ParkingSlotRepository;
import com.parking.backend.parking.SlotStatus;
import com.parking.backend.user.Role;
import com.parking.backend.user.User;
import com.parking.backend.user.UserRepository;
import com.parking.backend.vehicle.Vehicle;
import com.parking.backend.vehicle.VehicleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = "spring.datasource.url=${PARKING_TEST_JDBC_URL:jdbc:postgresql://localhost:5432/parking_test_db}")
class ReservationConcurrencyTest {

    private static final int THREADS = 5;

    @Autowired ReservationService service;
    @Autowired GateService gate;
    @Autowired com.parking.backend.gate.VisitController visits;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationRepository reservations;
    @Autowired VehicleRepository vehicles;
    @Autowired UserRepository users;
    @Autowired ParkingSlotRepository slots;
    @Autowired PasswordEncoder encoder;

    private final List<String> emails = new ArrayList<>();
    private final List<Long> vehicleIds = new ArrayList<>();
    private Long slotId;

    @BeforeEach
    void setUp() {
        ParkingSlot slot = slots.findAll().stream()
                .filter(s -> s.getCode().equals("A2") && s.getStatus() == SlotStatus.AVAILABLE)
                .findFirst().orElseThrow();
        slotId = slot.getId();
        String run = java.util.UUID.randomUUID().toString();
        int suffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(10000);

        // five different drivers, each with their own vehicle
        for (int i = 1; i <= THREADS; i++) {
            User u = new User();
            u.setName("Racer " + i);
            u.setEmail("race-" + run + "-" + i + "@test.local");
            u.setPasswordHash(encoder.encode("Passw0rd!x"));
            u.setRole(Role.DRIVER);
            u = users.save(u);

            Vehicle v = new Vehicle();
            v.setUser(u);
            v.setPlateNumber("TS0" + i + "AB" + String.format("%04d", suffix));
            vehicleIds.add(vehicles.save(v).getId());
            emails.add(u.getEmail());
        }
    }

    @Test
    void fiveDriversRaceForOneSlot_exactlyOneWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            final int n = i;
            results.add(pool.submit(() -> {
                ready.countDown();   // "I'm at the starting line"
                startGun.await();    // wait for the shot
                try {
                    service.reserve(emails.get(n),
                            new CreateReservationRequest(slotId, vehicleIds.get(n)));
                    return true;
                } catch (ConflictException e) {
                    return false;    // politely refused
                }
            }));
        }

        ready.await();       // everyone is at the line
        startGun.countDown(); // fire: all five run at once

        int wins = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                wins++;
            }
        }
        pool.shutdown();

        System.out.println("WINNERS: " + wins + " of " + THREADS);
        assertEquals(1, wins, "exactly one driver must get the slot");
        assertEquals(1, emails.stream().mapToLong(email -> reservations.findByUserEmailOrderByCreatedAtDesc(email).size()).sum(), "exactly one reservation row");
        assertEquals(SlotStatus.RESERVED, slots.findById(slotId).orElseThrow().getStatus());

        // Continue with the winning driver. Hold the admitted vehicle's slot lock until
        // PostgreSQL confirms that cancellation is waiting on this exact transaction.
        String winner = emails.stream().filter(email -> !reservations.findByUserEmailOrderByCreatedAtDesc(email).isEmpty()).findFirst().orElseThrow();
        Long reservationId = reservations.findByUserEmailOrderByCreatedAtDesc(winner).get(0).getId();
        String plate = vehicles.findById(vehicleIds.get(emails.indexOf(winner))).orElseThrow().getPlateNumber();
        DetectionRequest reading = new DetectionRequest(plate, 1.0);
        ExecutorService canceller = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> rejected = transactions.execute(tx -> {
                assertEquals(GateDecision.APPROVED, gate.enter(reading).decision());
                int backendPid = jdbc.queryForObject("select pg_backend_pid()", Integer.class);
                Future<Boolean> cancellation = canceller.submit(() -> {
                    try {
                        service.cancel(winner, reservationId);
                        return false;
                    } catch (ConflictException expected) {
                        return true;
                    }
                });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (!jdbc.queryForObject("select exists(select 1 from pg_stat_activity a "
                        + "where ? = any(pg_blocking_pids(a.pid)))", Boolean.class, backendPid)) {
                    assertTrue(System.nanoTime() < deadline, "Cancellation never waited for the gate lock");
                    java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
                }
                return cancellation;
            });
            assertTrue(rejected.get(10, TimeUnit.SECONDS));
            assertEquals(GateDecision.DENIED, gate.exit(reading).decision());
            assertEquals(SlotStatus.RESERVED, slots.findById(slotId).orElseThrow().getStatus());
            var held = visits.mine(() -> winner).get(0);
            var camera = new com.parking.backend.gate.VisitController.Observation(plate, 1.0, held.lotId(), held.bay(), true);
            visits.observe(camera);
            assertEquals(SlotStatus.OCCUPIED, slots.findById(slotId).orElseThrow().getStatus());
            visits.cash(held.id(), held.rupees());
            assertEquals(GateDecision.APPROVED, gate.exit(reading).decision());
            assertEquals(SlotStatus.AVAILABLE, slots.findById(slotId).orElseThrow().getStatus());
            assertEquals(GateDecision.APPROVED, gate.enter(reading).decision());
            var walkIn = visits.mine(() -> winner).get(0);
            org.junit.jupiter.api.Assertions.assertNull(walkIn.bay());
            visits.choose(() -> winner, walkIn.id(), slotId);
            visits.observe(new com.parking.backend.gate.VisitController.Observation(plate, 1.0, held.lotId(), held.section(), false));
            assertEquals(held.section(), visits.mine(() -> winner).get(0).checkpoint());
            visits.observe(camera);
            assertEquals(GateDecision.DENIED, gate.exit(reading).decision());
            visits.cash(walkIn.id(), walkIn.rupees());
            assertEquals(GateDecision.APPROVED, gate.exit(reading).decision());
            assertEquals(SlotStatus.AVAILABLE, slots.findById(slotId).orElseThrow().getStatus());
        } finally {
            canceller.shutdownNow();
        }
    }
}