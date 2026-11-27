package com.parking.backend.gate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    java.util.List<ParkingSession> findByVehicleUserEmailAndStatus(String email, SessionStatus status);
    java.util.List<ParkingSession> findByStatus(SessionStatus status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParkingSession s where s.id = :id")
    Optional<ParkingSession> lockVisit(@Param("id") Long id);

    Optional<ParkingSession> findByVehicleIdAndStatus(Long vehicleId, SessionStatus status);

    Optional<ParkingSession> findFirstByVehicleIdAndStatusOrderByExitTimeDesc(Long vehicleId, SessionStatus status);

    @Query("select s.id from ParkingSession s where s.vehicle.id = :vehicleId and s.status = :status")
    Optional<Long> findIdByVehicleAndStatus(@Param("vehicleId") Long vehicleId,
                                            @Param("status") SessionStatus status);

    @Query("select s.slot.id from ParkingSession s where s.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);
}