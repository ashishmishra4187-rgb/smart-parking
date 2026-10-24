package com.parking.backend.reservation;

import com.parking.backend.common.ConflictException;
import com.parking.backend.parking.RecommendationDto;

import java.util.List;

public class SlotUnavailableException extends ConflictException {

    private final List<RecommendationDto> alternatives;

    public SlotUnavailableException(String message, List<RecommendationDto> alternatives) {
        super(message);
        this.alternatives = alternatives;
    }

    public List<RecommendationDto> getAlternatives() {
        return alternatives;
    }
}