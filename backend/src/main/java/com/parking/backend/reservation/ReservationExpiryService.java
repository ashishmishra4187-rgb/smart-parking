package com.parking.backend.reservation;

import com.parking.backend.audit.AuditService;
import com.parking.backend.notification.NotificationService;
import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.ParkingSlotRepository;
import com.parking.backend.parking.SlotService;
import com.parking.backend.parking.SlotStatus;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationExpiryService {

    private final ReservationRepository reservations;
    private final ParkingSlotRepository slots;
    private final SlotService slotService;
    private final NotificationService notifications;
    private final AuditService audit;

    public ReservationExpiryService(ReservationRepository reservations, ParkingSlotRepository slots,
                                    SlotService slotService, NotificationService notifications,
                                    AuditService audit) {
        this.reservations = reservations;
        this.slots = slots;
        this.slotService = slotService;
        this.notifications = notifications;
        this.audit = audit;
    }

    /** Expires ONE reservation in its own transaction. */
    @Transactional
    public void expire(Long reservationId) {
        // 1. get only the slot id (this does not load the reservation yet)
        Long slotId = reservations.findSlotIdById(reservationId).orElse(null);
        if (slotId == null) {
            return;
        }
        // 2. lock the slot, same as reserve and cancel do
        ParkingSlot slot = slots.findByIdForUpdate(slotId).orElse(null);
        if (slot == null) {
            return;
        }
        // 3. NOW load the reservation: this is a fresh read, after any cancel has finished
        Reservation r = reservations.findById(reservationId).orElse(null);
        if (r == null || r.getStatus() != ReservationStatus.ACTIVE
                || r.getExpiresAt().isAfter(Instant.now())) {
            return; // cancelled or changed in the meantime: nothing to do
        }

        r.setStatus(ReservationStatus.NO_SHOW);
        if (slot.getStatus() == SlotStatus.RESERVED) {
            slotService.changeStatus(slot, SlotStatus.AVAILABLE);
        }

        String email = r.getUser().getEmail();
        long noShows = reservations.countByUserEmailAndStatus(email, ReservationStatus.NO_SHOW);
        notifications.create(r.getUser(), "RESERVATION_EXPIRED",
                "Your reservation " + r.getPublicCode() + " for slot " + slot.getCode()
                        + " expired. No-shows so far: " + noShows);
        audit.log("system", "EXPIRE", "reservation", r.getId(), "slot " + slot.getCode());
    }

    /** Sends the "2 minutes left" warning, once. */
    @Transactional
    public void warn(Long reservationId) {
        Reservation r = reservations.findById(reservationId).orElse(null);
        if (r == null || r.getStatus() != ReservationStatus.ACTIVE || r.isWarningSent()) {
            return;
        }
        r.setWarningSent(true);
        notifications.create(r.getUser(), "RESERVATION_WARNING",
                "Your reservation " + r.getPublicCode() + " for slot " + r.getSlot().getCode()
                        + " expires in about 2 minutes.");
    }
}