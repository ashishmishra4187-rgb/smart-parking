package com.parking.backend.manager;

import com.parking.backend.gate.GateDecision;
import com.parking.backend.gate.LicensePlateDetection;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DetectionQueryRepository extends JpaRepository<LicensePlateDetection, Long> {

    List<LicensePlateDetection> findAllByOrderByCreatedAtDesc(Pageable page);

    List<LicensePlateDetection> findByDecisionOrderByCreatedAtDesc(GateDecision decision, Pageable page);

    long countByDecisionAndCreatedAtAfter(GateDecision decision, Instant since);

    // flagged for review and not yet resolved (no manual_review row points at it)
    @Query("select d from LicensePlateDetection d where d.needsManualReview = true "
            + "and not exists (select m.id from ManualReview m where m.detection = d) "
            + "order by d.createdAt desc")
    List<LicensePlateDetection> findPendingReviews();

    @Query("select count(d) from LicensePlateDetection d where d.needsManualReview = true "
            + "and not exists (select m.id from ManualReview m where m.detection = d)")
    long countPendingReviews();
}