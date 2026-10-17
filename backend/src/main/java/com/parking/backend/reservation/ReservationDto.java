package com.parking.backend.reservation;

import java.time.Instant;

public record ReservationDto(Long id, String publicCode, String slotCode, String plateNumber,
                             ReservationStatus status, Instant createdAt, Instant expiresAt) {
}