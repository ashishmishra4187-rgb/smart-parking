package com.parking.backend.reservation;

import com.parking.backend.common.ConflictException;
import com.parking.backend.parking.SlotDto;
import java.util.List;

public class SlotUnavailableException extends ConflictException {

    private final List<SlotDto> alternatives;

    public SlotUnavailableException(String message, List<SlotDto> alternatives) {
        super(message);
        this.alternatives = alternatives;
    }

    public List<SlotDto> getAlternatives() {
        return alternatives;
    }
}