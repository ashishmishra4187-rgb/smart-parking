package com.parking.backend.parking;

import java.util.List;

public record RecommendationDto(
        Long slotId,
        String code,
        String section,
        String slotType,
        int score,
        List<String> reasons) {
}