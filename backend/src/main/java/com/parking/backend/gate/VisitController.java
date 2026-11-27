package com.parking.backend.gate;

import com.parking.backend.common.ConflictException;
import com.parking.backend.common.NotFoundException;
import com.parking.backend.parking.*;
import com.parking.backend.reservation.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Transactional
public class VisitController {
    private final ParkingSessionRepository sessions;
    private final ParkingSlotRepository slots;
    private final SlotService slotService;
    private final PlateLookupRepository vehicles;
    private final int hourlyRate;
    private final double confidence;

    public VisitController(ParkingSessionRepository sessions, ParkingSlotRepository slots, SlotService slotService,
                           PlateLookupRepository vehicles, @Value("${app.billing.hourly-inr:30}") int hourlyRate,
                           @Value("${app.gate.min-confidence:0.60}") double confidence) {
        this.sessions = sessions;
        this.slots = slots;
        this.slotService = slotService;
        this.vehicles = vehicles;
        if (hourlyRate <= 0) throw new IllegalArgumentException("Parking rate must be positive");
        this.hourlyRate = hourlyRate;
        this.confidence = confidence;
    }

    public record Visit(long id, long lotId, String plate, String bay, String section, String checkpoint,
                 boolean parked, boolean paid, int rupees) {}
    public record Observation(@NotBlank String plate, @NotNull @DecimalMin("0") @DecimalMax("1") Double confidence,
                       @Positive long lotId, @NotBlank String checkpoint, boolean bayCamera) {}

    private Visit describe(ParkingSession s) {
        var slot = s.getSlot();
        long seconds = Duration.between(s.getEntryTime(), Instant.now()).getSeconds();
        int fee = s.getPaidAt() == null ? Math.toIntExact(Math.max(1, (seconds + 3599) / 3600) * hourlyRate) : s.getPaidRupees();
        return new Visit(s.getId(), s.lotId(), s.getVehicle().getPlateNumber(), s.slotCode(),
                slot == null ? null : slot.getSection().getCode(), s.getCheckpoint(),
                slot != null && slot.getStatus() == SlotStatus.OCCUPIED, s.getPaidAt() != null, fee);
    }

    private ParkingSession active(long id) {
        return sessions.lockVisit(id).filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Active visit not found"));
    }

    @GetMapping("/api/visits")
    public List<Visit> mine(Principal principal) {
        return sessions.findByVehicleUserEmailAndStatus(principal.getName(), SessionStatus.ACTIVE)
                .stream().map(this::describe).toList();
    }

    @PostMapping("/api/visits/{id}/bay/{slotId}")
    public Visit choose(Principal principal, @PathVariable long id, @PathVariable long slotId) {
        var s = active(id);
        if (!s.getVehicle().getUser().getEmail().equals(principal.getName())) throw new NotFoundException("Visit not found");
        if (s.getSlot() != null || s.getPaidAt() != null) throw new ConflictException("Visit already assigned or paid");
        var slot = slots.findByIdForUpdate(slotId).orElseThrow(() -> new NotFoundException("Bay not found"));
        if (!slot.getSection().getLot().getId().equals(s.lotId()) || slot.getStatus() != SlotStatus.AVAILABLE
                || !ReservationService.fits(s.getVehicle().getType(), slot.getSlotType()))
            throw new ConflictException("Choose an available bay compatible with this vehicle");
        s.setSlot(slot);
        slotService.changeStatus(slot, SlotStatus.RESERVED);
        return describe(s);
    }

    @PostMapping("/api/detection/checkpoint")
    public GateResponse observe(@Valid @RequestBody Observation reading) {
        String plate = reading.plate().strip().toUpperCase(java.util.Locale.ROOT);
        if (reading.confidence() < confidence) throw new ConflictException("Plate confidence too low");
        var vehicle = vehicles.findByPlateNumber(plate).orElseThrow(() -> new NotFoundException("Unknown plate"));
        long id = sessions.findIdByVehicleAndStatus(vehicle.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("No active visit"));
        var s = active(id);
        if (s.lotId() != reading.lotId()) throw new ConflictException("Wrong facility");
        var mapped = slots.findAllByLotId(s.lotId()).stream().anyMatch(slot -> reading.bayCamera()
                ? slot.getCode().equals(reading.checkpoint()) : slot.getSection().getCode().equals(reading.checkpoint()));
        if (!mapped) throw new ConflictException("Unknown camera position");
        if (reading.bayCamera()) {
            if (!reading.checkpoint().equals(s.slotCode())) throw new ConflictException("Vehicle is outside assigned bay");
            var slot = slots.findByIdForUpdate(s.getSlot().getId()).orElseThrow();
            if (slot.getStatus() == SlotStatus.RESERVED) slotService.changeStatus(slot, SlotStatus.OCCUPIED);
        }
        s.setCheckpoint(reading.checkpoint());
        return new GateResponse(null, GateDecision.APPROVED, "Location updated", plate, null, s.slotCode(), false);
    }

    @GetMapping("/api/manager/visits")
    public List<Visit> cashier() { return sessions.findByStatus(SessionStatus.ACTIVE).stream().map(this::describe).toList(); }

    @PostMapping("/api/manager/visits/{id}/cash/{amount}")
    public Visit cash(@PathVariable long id, @PathVariable int amount) {
        var s = active(id);
        if (s.getPaidAt() == null) {
            if (amount != describe(s).rupees()) throw new ConflictException("Fee changed. Refresh before confirming payment");
            s.recordPayment(amount);
        }
        return describe(s);
    }
}
