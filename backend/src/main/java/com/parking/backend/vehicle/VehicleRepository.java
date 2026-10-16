package com.parking.backend.vehicle;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByPlateNumber(String plateNumber);
    List<Vehicle> findByUserEmail(String email);
    Optional<Vehicle> findByIdAndUserEmail(Long id, String email);
}