package com.parking.backend.manager;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualReviewRepository extends JpaRepository<ManualReview, Long> {
    boolean existsByDetectionId(Long detectionId);
}