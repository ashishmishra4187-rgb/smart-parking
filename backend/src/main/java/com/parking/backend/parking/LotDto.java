package com.parking.backend.parking;

public record LotDto(Long id, String name, String address, int reservationMinutes,
                     long total, long available, long reserved,
                     long occupied, long maintenance) {
}