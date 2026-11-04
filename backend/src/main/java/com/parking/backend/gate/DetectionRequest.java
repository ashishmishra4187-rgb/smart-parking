package com.parking.backend.gate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DetectionRequest(
        @NotBlank @Size(max = 30) String plate,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence) {
}