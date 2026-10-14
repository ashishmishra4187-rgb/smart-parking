package com.parking.backend.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateVehicleRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}[ -]?\\d{2}[ -]?[A-Za-z]{1,3}[ -]?\\d{4}$",
                message = "must look like DL01AB1234")
        String plateNumber,
        @NotNull VehicleType type) {
}