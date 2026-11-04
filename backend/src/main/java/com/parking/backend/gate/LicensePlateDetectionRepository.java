package com.parking.backend.gate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LicensePlateDetectionRepository extends JpaRepository<LicensePlateDetection, Long> {
}