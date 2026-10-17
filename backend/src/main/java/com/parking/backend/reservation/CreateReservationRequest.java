package com.parking.backend.reservation;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(@NotNull Long slotId, @NotNull Long vehicleId) {
}