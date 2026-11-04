package com.parking.backend.gate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    Optional<ParkingSession> findByVehicleIdAndStatus(Long vehicleId, SessionStatus status);

    Optional<ParkingSession> findFirstByVehicleIdAndStatusOrderByExitTimeDesc(Long vehicleId, SessionStatus status);

    @Query("select s.id from ParkingSession s where s.vehicle.id = :vehicleId and s.status = :status")
    Optional<Long> findIdByVehicleAndStatus(@Param("vehicleId") Long vehicleId,
                                            @Param("status") SessionStatus status);

    @Query("select s.slot.id from ParkingSession s where s.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);
}