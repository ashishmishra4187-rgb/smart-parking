package com.parking.backend.parking;

import com.parking.backend.common.ConflictException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SlotService {

    private static final Map<SlotStatus, Set<SlotStatus>> ALLOWED = Map.of(
            SlotStatus.AVAILABLE, EnumSet.of(SlotStatus.RESERVED, SlotStatus.MAINTENANCE),
            SlotStatus.RESERVED, EnumSet.of(SlotStatus.AVAILABLE, SlotStatus.OCCUPIED),
            SlotStatus.OCCUPIED, EnumSet.of(SlotStatus.AVAILABLE),
            SlotStatus.MAINTENANCE, EnumSet.of(SlotStatus.AVAILABLE));

    public void changeStatus(ParkingSlot slot, SlotStatus next) {
        if (!ALLOWED.get(slot.getStatus()).contains(next)) {
            throw new ConflictException(
                    "Slot " + slot.getCode() + " cannot go from " + slot.getStatus() + " to " + next);
        }
        slot.setStatus(next);
        slot.setUpdatedAt(Instant.now());
    }
}