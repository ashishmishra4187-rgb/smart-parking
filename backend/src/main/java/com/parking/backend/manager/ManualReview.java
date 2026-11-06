package com.parking.backend.manager;

import com.parking.backend.gate.LicensePlateDetection;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "manual_review", uniqueConstraints = @UniqueConstraint(columnNames = "detection_id"))
public class ManualReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detection_id")
    private LicensePlateDetection detection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewStatus status;

    @Column(name = "final_plate", length = 30)
    private String finalPlate;

    @Column(length = 300)
    private String note;

    @Column(name = "reviewed_by", nullable = false, length = 150)
    private String reviewedBy;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt = Instant.now();

    public Long getId() { return id; }
    public LicensePlateDetection getDetection() { return detection; }
    public void setDetection(LicensePlateDetection detection) { this.detection = detection; }
    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }
    public String getFinalPlate() { return finalPlate; }
    public void setFinalPlate(String finalPlate) { this.finalPlate = finalPlate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
}