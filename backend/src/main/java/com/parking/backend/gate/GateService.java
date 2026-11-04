package com.parking.backend.gate;

import com.parking.backend.audit.AuditService;
import com.parking.backend.common.NotFoundException;
import com.parking.backend.notification.NotificationService;
import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.ParkingSlotRepository;
import com.parking.backend.parking.SlotService;
import com.parking.backend.parking.SlotStatus;
import com.parking.backend.reservation.Reservation;
import com.parking.backend.reservation.ReservationStatus;
import com.parking.backend.vehicle.Vehicle;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GateService {

    // same shape as vehicle plates: DL01AB1234
    private static final Pattern PLATE = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z]{1,3}\\d{4}$");

    private final PlateLookupRepository vehicles;
    private final GateReservationRepository reservations;
    private final ParkingSessionRepository sessions;
    private final ParkingSlotRepository slots;
    private final LicensePlateDetectionRepository detections;
    private final EntryExitLogRepository logs;
    private final SlotService slotService;
    private final NotificationService notifications;
    private final AuditService audit;
    private final double minConfidence;
    private final long debounceSeconds;

    public GateService(PlateLookupRepository vehicles, GateReservationRepository reservations,
                       ParkingSessionRepository sessions, ParkingSlotRepository slots,
                       LicensePlateDetectionRepository detections, EntryExitLogRepository logs,
                       SlotService slotService, NotificationService notifications, AuditService audit,
                       @Value("${app.gate.min-confidence:0.60}") double minConfidence,
                       @Value("${app.gate.debounce-seconds:15}") long debounceSeconds) {
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.sessions = sessions;
        this.slots = slots;
        this.detections = detections;
        this.logs = logs;
        this.slotService = slotService;
        this.notifications = notifications;
        this.audit = audit;
        this.minConfidence = minConfidence;
        this.debounceSeconds = debounceSeconds;
    }

    // ---------------------------------------------------------------- ENTRY
    @Transactional
    public GateResponse enter(DetectionRequest req) {
        Instant now = Instant.now();
        String plate = clean(req.plate());
        GateDirection dir = GateDirection.ENTRY;

        if (unclear(req, plate)) {
            return finish(req, plate, dir, GateDecision.RETRY, "Plate unclear: manual review needed", true, null, null);
        }
        Vehicle vehicle = vehicles.findByPlateNumber(plate).orElse(null);
        if (vehicle == null) {
            return finish(req, plate, dir, GateDecision.DENIED, "Vehicle not registered", false, null, null);
        }

        // already inside? a repeat of the same event within a few seconds is only a camera echo
        ParkingSession open = sessions.findByVehicleIdAndStatus(vehicle.getId(), SessionStatus.ACTIVE).orElse(null);
        if (open != null) {
            boolean echo = open.getEntryTime().isAfter(now.minusSeconds(debounceSeconds));
            String code = open.getReservation() != null ? open.getReservation().getPublicCode() : null;
            return finish(req, plate, dir, echo ? GateDecision.APPROVED : GateDecision.DENIED,
                    echo ? "Duplicate event ignored" : "Vehicle is already inside",
                    false, code, open.getSlot().getCode());
        }

        // 1. ids only, then LOCK the slot, 2. only now load the reservation fresh
        Long reservationId = reservations.findIdByVehicleAndStatus(vehicle.getId(), ReservationStatus.ACTIVE).orElse(null);
        if (reservationId == null) {
            return finish(req, plate, dir, GateDecision.DENIED, "NO VALID RESERVATION", false, null, null);
        }
        Long slotId = reservations.findSlotIdById(reservationId).orElseThrow(() -> new NotFoundException("Slot not found"));
        ParkingSlot slot = slots.findByIdForUpdate(slotId).orElseThrow(() -> new NotFoundException("Slot not found"));
        Reservation r = reservations.findById(reservationId).orElse(null);

        if (r == null || r.getStatus() != ReservationStatus.ACTIVE) {
            return finish(req, plate, dir, GateDecision.DENIED, "NO VALID RESERVATION", false, null, null);
        }
        if (r.getExpiresAt().isBefore(now)) {
            return finish(req, plate, dir, GateDecision.DENIED, "Reservation expired", false, r.getPublicCode(), slot.getCode());
        }
        if (slot.getStatus() != SlotStatus.RESERVED) {
            return finish(req, plate, dir, GateDecision.DENIED, "Slot is not reserved", false, r.getPublicCode(), slot.getCode());
        }

        // all checks passed: ONE transaction changes everything
        r.setStatus(ReservationStatus.COMPLETED);
        slotService.changeStatus(slot, SlotStatus.OCCUPIED);      // also broadcasts the live event after commit

        ParkingSession s = new ParkingSession();
        s.setVehicle(vehicle);
        s.setSlot(slot);
        s.setReservation(r);
        s.setEntryTime(now);
        s = sessions.save(s);

        saveLog(s, plate, dir, "Reservation " + r.getPublicCode() + ", slot " + slot.getCode());
        notifications.create(r.getUser(), "GATE_ENTRY",
                "Welcome! The gate opened for " + plate + ". Your slot is " + slot.getCode() + ".");
        audit.log("gate", "ENTRY", "session", s.getId(), "slot " + slot.getCode() + ", plate " + plate);
        return finish(req, plate, dir, GateDecision.APPROVED, "Entry approved", false, r.getPublicCode(), slot.getCode());
    }

    // ----------------------------------------------------------------- EXIT
    @Transactional
    public GateResponse exit(DetectionRequest req) {
        Instant now = Instant.now();
        String plate = clean(req.plate());
        GateDirection dir = GateDirection.EXIT;

        if (unclear(req, plate)) {
            return finish(req, plate, dir, GateDecision.RETRY, "Plate unclear: manual review needed", true, null, null);
        }
        Vehicle vehicle = vehicles.findByPlateNumber(plate).orElse(null);
        if (vehicle == null) {
            return finish(req, plate, dir, GateDecision.DENIED, "Vehicle not registered", false, null, null);
        }

        Long sessionId = sessions.findIdByVehicleAndStatus(vehicle.getId(), SessionStatus.ACTIVE).orElse(null);
        if (sessionId == null) {
            ParkingSession last = sessions
                    .findFirstByVehicleIdAndStatusOrderByExitTimeDesc(vehicle.getId(), SessionStatus.COMPLETED).orElse(null);
            boolean echo = last != null && last.getExitTime() != null
                    && last.getExitTime().isAfter(now.minusSeconds(debounceSeconds));
            return finish(req, plate, dir, echo ? GateDecision.APPROVED : GateDecision.DENIED,
                    echo ? "Duplicate event ignored" : "No active session for this vehicle", false, null, null);
        }

        Long slotId = sessions.findSlotIdById(sessionId).orElseThrow(() -> new NotFoundException("Slot not found"));
        ParkingSlot slot = slots.findByIdForUpdate(slotId).orElseThrow(() -> new NotFoundException("Slot not found"));
        ParkingSession s = sessions.findById(sessionId).orElse(null);       // fresh read after the lock
        if (s == null || s.getStatus() != SessionStatus.ACTIVE) {
            return finish(req, plate, dir, GateDecision.DENIED, "No active session for this vehicle", false, null, null);
        }

        s.setExitTime(now);
        s.setDurationMinutes((int) Duration.between(s.getEntryTime(), now).toMinutes());
        s.setStatus(SessionStatus.COMPLETED);
        if (slot.getStatus() == SlotStatus.OCCUPIED) {
            slotService.changeStatus(slot, SlotStatus.AVAILABLE);
        }

        saveLog(s, plate, dir, "Stayed " + s.getDurationMinutes() + " minutes in " + slot.getCode());
        notifications.create(vehicle.getUser(), "GATE_EXIT",
                "Exit recorded for " + plate + ". You stayed " + s.getDurationMinutes() + " minutes.");
        audit.log("gate", "EXIT", "session", s.getId(), "slot " + slot.getCode() + ", plate " + plate);
        return finish(req, plate, dir, GateDecision.APPROVED, "Exit approved", false, null, slot.getCode());
    }

    // -------------------------------------------------------------- helpers
    private String clean(String raw) {
        return raw.trim().replaceAll("[ -]", "").toUpperCase();
    }

    private boolean unclear(DetectionRequest req, String plate) {
        return req.confidence() < minConfidence || !PLATE.matcher(plate).matches();
    }

    private void saveLog(ParkingSession s, String plate, GateDirection dir, String details) {
        EntryExitLog log = new EntryExitLog();
        log.setSession(s);
        log.setPlate(plate);
        log.setDirection(dir);
        log.setDetails(details);
        logs.save(log);
    }

    // EVERY detection is saved, whatever the decision
    private GateResponse finish(DetectionRequest req, String plate, GateDirection dir, GateDecision decision,
                                String reason, boolean review, String reservationCode, String slotCode) {
        LicensePlateDetection d = new LicensePlateDetection();
        d.setPlateRaw(req.plate().trim());
        d.setPlateClean(plate);
        d.setConfidence(req.confidence());
        d.setDirection(dir);
        d.setDecision(decision);
        d.setReason(reason);
        d.setNeedsManualReview(review);
        d = detections.save(d);
        return new GateResponse(d.getId(), decision, reason, plate, reservationCode, slotCode, review);
    }
}