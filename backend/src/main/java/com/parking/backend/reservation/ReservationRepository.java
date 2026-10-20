package com.parking.backend.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByVehicleIdAndStatus(Long vehicleId, ReservationStatus status);

    Optional<Reservation> findByIdAndUserEmail(Long id, String email);

    List<Reservation> findByUserEmailOrderByCreatedAtDesc(String email);

    long countByUserEmailAndStatus(String email, ReservationStatus status);

    @Query("select r.id from Reservation r where r.status = :status and r.expiresAt < :now")
    List<Long> findIdsToExpire(@Param("status") ReservationStatus status, @Param("now") Instant now);

    @Query("select r.id from Reservation r where r.status = :status and r.warningSent = false "
            + "and r.expiresAt > :now and r.expiresAt <= :threshold")
    List<Long> findIdsToWarn(@Param("status") ReservationStatus status,
                             @Param("now") Instant now, @Param("threshold") Instant threshold);

    @Query("select r.slot.id from Reservation r where r.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);
}