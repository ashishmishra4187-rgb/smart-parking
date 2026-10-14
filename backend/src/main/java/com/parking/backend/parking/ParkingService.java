package com.parking.backend.parking;

import com.parking.backend.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParkingService {

    private final ParkingLotRepository lots;
    private final ParkingSlotRepository slots;

    public ParkingService(ParkingLotRepository lots, ParkingSlotRepository slots) {
        this.lots = lots;
        this.slots = slots;
    }

    public List<LotDto> getLots() {
        return lots.findAll().stream().map(this::toDto).toList();
    }

    public List<SlotDto> getSlots(Long lotId) {
        if (!lots.existsById(lotId)) {
            throw new NotFoundException("Lot " + lotId + " not found");
        }
        return slots.findAllByLotId(lotId).stream().map(SlotDto::from).toList();
    }

    private LotDto toDto(ParkingLot lot) {
        Long id = lot.getId();
        return new LotDto(id, lot.getName(), lot.getAddress(), lot.getReservationMinutes(),
                slots.countBySectionLotId(id),
                slots.countBySectionLotIdAndStatus(id, SlotStatus.AVAILABLE),
                slots.countBySectionLotIdAndStatus(id, SlotStatus.RESERVED),
                slots.countBySectionLotIdAndStatus(id, SlotStatus.OCCUPIED),
                slots.countBySectionLotIdAndStatus(id, SlotStatus.MAINTENANCE));
    }
}