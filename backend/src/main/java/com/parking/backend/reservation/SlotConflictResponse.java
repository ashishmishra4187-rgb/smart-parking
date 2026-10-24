package com.parking.backend.reservation;

import com.parking.backend.parking.RecommendationDto;

import java.time.Instant;
import java.util.List;

public record SlotConflictResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<RecommendationDto> alternatives) {
}