package com.parking.backend.parking;

import java.time.Instant;

public record SlotEvent(Long slotId, String code, SlotStatus status, Instant updatedAt) {
}