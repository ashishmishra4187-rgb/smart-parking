package com.parking.backend.manager;

import com.parking.backend.parking.ParkingSlot;
import com.parking.backend.parking.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlotCountRepository extends JpaRepository<ParkingSlot, Long> {
    long countByStatus(SlotStatus status);
}