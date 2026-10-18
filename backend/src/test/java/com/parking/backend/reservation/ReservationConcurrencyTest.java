package com.parking.backend.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.parking.backend.common.ConflictException;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/parking_test_db")
class ReservationConcurrencyTest {

    private static final int THREADS = 5;

    @Autowired ReservationService service;
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
        // start from a clean state (this is the TEST database, not your real one)
        reservations.deleteAll();
        vehicles.deleteAll();
        for (int i = 1; i <= THREADS; i++) {
            users.findByEmail("race" + i + "@test.local").ifPresent(users::delete);
        }
        emails.clear();
        vehicleIds.clear();

        // make sure slot A2 is free
        ParkingSlot slot = slots.findAll().stream()
                .filter(s -> s.getCode().equals("A2")).findFirst().orElseThrow();
        slot.setStatus(SlotStatus.AVAILABLE);
        slots.save(slot);
        slotId = slot.getId();

        // five different drivers, each with their own vehicle
        for (int i = 1; i <= THREADS; i++) {
            User u = new User();
            u.setName("Racer " + i);
            u.setEmail("race" + i + "@test.local");
            u.setPasswordHash(encoder.encode("Passw0rd!x"));
            u.setRole(Role.DRIVER);
            u = users.save(u);

            Vehicle v = new Vehicle();
            v.setUser(u);
            v.setPlateNumber("TS01AB000" + i);
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
        assertEquals(1, reservations.count(), "exactly one reservation row");
        assertEquals(SlotStatus.RESERVED, slots.findById(slotId).orElseThrow().getStatus());
    }
}