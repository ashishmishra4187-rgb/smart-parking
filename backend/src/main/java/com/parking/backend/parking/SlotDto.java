package com.parking.backend.parking;

public record SlotDto(Long id, String code, String section,
                      SlotType slotType, SlotStatus status, double x, double y) {

    public static SlotDto from(ParkingSlot s) {
        return new SlotDto(s.getId(), s.getCode(), s.getSection().getCode(),
                s.getSlotType(), s.getStatus(), s.getX(), s.getY());
    }
}