package com.parking.backend.gate;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "license_plate_detection")
public class LicensePlateDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_raw", nullable = false, length = 30)
    private String plateRaw;

    @Column(name = "plate_clean", length = 30)
    private String plateClean;

    @Column(nullable = false)
    private double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GateDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GateDecision decision;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(name = "needs_manual_review", nullable = false)
    private boolean needsManualReview;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getPlateRaw() { return plateRaw; }
    public void setPlateRaw(String plateRaw) { this.plateRaw = plateRaw; }
    public String getPlateClean() { return plateClean; }
    public void setPlateClean(String plateClean) { this.plateClean = plateClean; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public GateDirection getDirection() { return direction; }
    public void setDirection(GateDirection direction) { this.direction = direction; }
    public GateDecision getDecision() { return decision; }
    public void setDecision(GateDecision decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isNeedsManualReview() { return needsManualReview; }
    public void setNeedsManualReview(boolean needsManualReview) { this.needsManualReview = needsManualReview; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}