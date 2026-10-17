package com.parking.backend.reservation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByVehicleIdAndStatus(Long vehicleId, ReservationStatus status);
    Optional<Reservation> findByIdAndUserEmail(Long id, String email);
    List<Reservation> findByUserEmailOrderByCreatedAtDesc(String email);
}