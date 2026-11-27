package com.parking.backend.gate;

import com.parking.backend.vehicle.Vehicle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlateLookupRepository extends JpaRepository<Vehicle, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Vehicle> findByPlateNumber(String plateNumber);
}