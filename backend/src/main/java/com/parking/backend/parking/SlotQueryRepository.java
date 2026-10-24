package com.parking.backend.parking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SlotQueryRepository extends JpaRepository<ParkingSlot, Long> {

    // one query: all slots of a lot, with section and lot loaded together
    @Query("select s from ParkingSlot s join fetch s.section sec join fetch sec.lot l where l.id = :lotId")
    List<ParkingSlot> findAllInLot(@Param("lotId") Long lotId);
}