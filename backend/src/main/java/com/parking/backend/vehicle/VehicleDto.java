package com.parking.backend.vehicle;

public record VehicleDto(Long id, String plateNumber, VehicleType type, boolean primaryVehicle) {
}