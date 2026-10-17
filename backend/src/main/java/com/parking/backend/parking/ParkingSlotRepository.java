package com.parking.backend.parking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    @Query("select s from ParkingSlot s join fetch s.section sec "
            + "where sec.lot.id = :lotId order by sec.code, s.code")
    List<ParkingSlot> findAllByLotId(@Param("lotId") Long lotId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSlot s where s.id = :id")
    Optional<ParkingSlot> findByIdForUpdate(@Param("id") Long id);
    long countBySectionLotId(Long lotId);

    long countBySectionLotIdAndStatus(Long lotId, SlotStatus status);
}