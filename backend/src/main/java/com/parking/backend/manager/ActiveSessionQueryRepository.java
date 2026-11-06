package com.parking.backend.manager;

import com.parking.backend.gate.ParkingSession;
import com.parking.backend.gate.SessionStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActiveSessionQueryRepository extends JpaRepository<ParkingSession, Long> {

    @Query("select s from ParkingSession s join fetch s.vehicle join fetch s.slot "
            + "where s.status = :status order by s.entryTime desc")
    List<ParkingSession> findByStatusWithDetails(@Param("status") SessionStatus status);

    long countByStatus(SessionStatus status);

    long countByEntryTimeAfter(Instant since);
}