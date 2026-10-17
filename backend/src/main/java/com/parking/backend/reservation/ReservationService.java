package com.parking.backend.reservation;

import com.parking.backend.audit.AuditService;
import com.parking.backend.common.ConflictException;
import com.parking.backend.common.NotFoundException;
import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.ParkingSlotRepository;
import com.parking.backend.parking.SlotService;
import com.parking.backend.parking.SlotStatus;
import com.parking.backend.parking.SlotType;
import com.parking.backend.user.User;
import com.parking.backend.user.UserRepository;
import com.parking.backend.vehicle.Vehicle;
import com.parking.backend.vehicle.VehicleRepository;
import com.parking.backend.vehicle.VehicleType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final ParkingSlotRepository slots;
    private final VehicleRepository vehicles;
    private final UserRepository users;
    private final SlotService slotService;
    private final AuditService audit;

    public ReservationService(ReservationRepository reservations, ParkingSlotRepository slots,
                              VehicleRepository vehicles, UserRepository users,
                              SlotService slotService, AuditService audit) {
        this.reservations = reservations;
        this.slots = slots;
        this.vehicles = vehicles;
        this.users = users;
        this.slotService = slotService;
        this.audit = audit;
    }

    @Transactional
    public ReservationDto reserve(String email, CreateReservationRequest req) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Instant now = Instant.now();
        if (user.isReservationRestricted()
                && (user.getRestrictedUntil() == null || user.getRestrictedUntil().isAfter(now))) {
            throw new ConflictException("Reservations are restricted for this account");
        }

        Vehicle vehicle = vehicles.findByIdAndUserEmail(req.vehicleId(), email)
                .orElseThrow(() -> new NotFoundException("Vehicle " + req.vehicleId() + " not found"));
        if (reservations.existsByVehicleIdAndStatus(vehicle.getId(), ReservationStatus.ACTIVE)) {
            throw new ConflictException("Vehicle " + vehicle.getPlateNumber() + " already has an active reservation");
        }

        // LOCK the slot row: other requests for this slot now wait here
        ParkingSlot slot = slots.findByIdForUpdate(req.slotId())
                .orElseThrow(() -> new NotFoundException("Slot " + req.slotId() + " not found"));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ConflictException("Slot " + slot.getCode() + " is no longer available");
        }
        if (!fits(vehicle.getType(), slot.getSlotType())) {
            throw new ConflictException("A " + vehicle.getType() + " cannot use a " + slot.getSlotType() + " slot");
        }

        slotService.changeStatus(slot, SlotStatus.RESERVED);

        int minutes = slot.getSection().getLot().getReservationMinutes();
        Reservation r = new Reservation();
        r.setPublicCode("R" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        r.setUser(user);
        r.setVehicle(vehicle);
        r.setSlot(slot);
        r.setExpiresAt(now.plus(minutes, ChronoUnit.MINUTES));
        r = reservations.save(r);

        audit.log(email, "RESERVE", "reservation", r.getId(), "slot " + slot.getCode());
        return toDto(r);
    }

    @Transactional
    public void cancel(String email, Long id) {
        Reservation r = reservations.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new NotFoundException("Reservation " + id + " not found"));
        ParkingSlot slot = slots.findByIdForUpdate(r.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("Slot not found"));
        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new ConflictException("Only an active reservation can be cancelled");
        }
        r.setStatus(ReservationStatus.CANCELLED);
        slotService.changeStatus(slot, SlotStatus.AVAILABLE);
        audit.log(email, "CANCEL", "reservation", r.getId(), "slot " + slot.getCode());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> mine(String email) {
        return reservations.findByUserEmailOrderByCreatedAtDesc(email).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ReservationDto get(String email, Long id) {
        return reservations.findByIdAndUserEmail(id, email).map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Reservation " + id + " not found"));
    }

    private boolean fits(VehicleType v, SlotType s) {
        return switch (v) {
            case BIKE -> s == SlotType.BIKE;
            case EV -> s == SlotType.EV || s == SlotType.CAR || s == SlotType.VISITOR;
            case CAR -> s == SlotType.CAR || s == SlotType.VISITOR;
        };
    }

    private ReservationDto toDto(Reservation r) {
        return new ReservationDto(r.getId(), r.getPublicCode(), r.getSlot().getCode(),
                r.getVehicle().getPlateNumber(), r.getStatus(), r.getCreatedAt(), r.getExpiresAt());
    }
}