package com.parking.backend.manager;

import com.parking.backend.reservation.Reservation;
import com.parking.backend.reservation.ReservationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActiveReservationQueryRepository extends JpaRepository<Reservation, Long> {

    @Query("select r from Reservation r join fetch r.vehicle join fetch r.slot "
            + "where r.status = :status order by r.expiresAt")
    List<Reservation> findByStatusWithDetails(@Param("status") ReservationStatus status);

    long countByStatus(ReservationStatus status);
}