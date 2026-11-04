package com.parking.backend.gate;

import com.parking.backend.reservation.Reservation;
import com.parking.backend.reservation.ReservationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GateReservationRepository extends JpaRepository<Reservation, Long> {

    // ids only: nothing is loaded into the persistence context yet
    @Query("select r.id from Reservation r where r.vehicle.id = :vehicleId and r.status = :status")
    Optional<Long> findIdByVehicleAndStatus(@Param("vehicleId") Long vehicleId,
                                            @Param("status") ReservationStatus status);

    @Query("select r.slot.id from Reservation r where r.id = :id")
    Optional<Long> findSlotIdById(@Param("id") Long id);
}