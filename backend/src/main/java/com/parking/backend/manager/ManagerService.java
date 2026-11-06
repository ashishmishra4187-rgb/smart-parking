package com.parking.backend.manager;

import com.parking.backend.audit.AuditService;
import com.parking.backend.common.ConflictException;
import com.parking.backend.common.NotFoundException;
import com.parking.backend.gate.DetectionRequest;
import com.parking.backend.gate.GateDecision;
import com.parking.backend.gate.GateDirection;
import com.parking.backend.gate.GateResponse;
import com.parking.backend.gate.GateService;
import com.parking.backend.gate.LicensePlateDetection;
import com.parking.backend.gate.SessionStatus;
import com.parking.backend.manager.ManagerDtos.ActiveReservationDto;
import com.parking.backend.manager.ManagerDtos.ActiveSessionDto;
import com.parking.backend.manager.ManagerDtos.DetectionDto;
import com.parking.backend.manager.ManagerDtos.OverviewDto;
import com.parking.backend.manager.ManagerDtos.ReviewOutcomeDto;
import com.parking.backend.parking.SlotStatus;
import com.parking.backend.reservation.ReservationStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerService {

    private final SlotCountRepository slotCounts;
    private final ActiveSessionQueryRepository sessions;
    private final ActiveReservationQueryRepository reservations;
    private final DetectionQueryRepository detections;
    private final ManualReviewRepository reviews;
    private final GateService gate;
    private final AuditService audit;

    public ManagerService(SlotCountRepository slotCounts, ActiveSessionQueryRepository sessions,
                          ActiveReservationQueryRepository reservations, DetectionQueryRepository detections,
                          ManualReviewRepository reviews, GateService gate, AuditService audit) {
        this.slotCounts = slotCounts;
        this.sessions = sessions;
        this.reservations = reservations;
        this.detections = detections;
        this.reviews = reviews;
        this.gate = gate;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ reads
    @Transactional(readOnly = true)
    public OverviewDto overview() {
        Instant now = Instant.now();
        Instant since = now.minus(24, ChronoUnit.HOURS);
        return new OverviewDto(
                slotCounts.count(),
                slotCounts.countByStatus(SlotStatus.AVAILABLE),
                slotCounts.countByStatus(SlotStatus.RESERVED),
                slotCounts.countByStatus(SlotStatus.OCCUPIED),
                slotCounts.countByStatus(SlotStatus.MAINTENANCE),
                reservations.countByStatus(ReservationStatus.ACTIVE),
                sessions.countByStatus(SessionStatus.ACTIVE),
                sessions.countByEntryTimeAfter(since),
                detections.countByDecisionAndCreatedAtAfter(GateDecision.DENIED, since),
                detections.countByDecisionAndCreatedAtAfter(GateDecision.RETRY, since),
                detections.countPendingReviews(),
                now);
    }

    @Transactional(readOnly = true)
    public List<ActiveSessionDto> carsInside() {
        Instant now = Instant.now();
        return sessions.findByStatusWithDetails(SessionStatus.ACTIVE).stream()
                .map(s -> new ActiveSessionDto(s.getId(), s.getVehicle().getPlateNumber(), s.getSlot().getCode(),
                        s.getEntryTime(), Duration.between(s.getEntryTime(), now).toMinutes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActiveReservationDto> activeReservations() {
        return reservations.findByStatusWithDetails(ReservationStatus.ACTIVE).stream()
                .map(r -> new ActiveReservationDto(r.getId(), r.getPublicCode(), r.getSlot().getCode(),
                        r.getVehicle().getPlateNumber(), r.getExpiresAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DetectionDto> recentDetections(GateDecision decision, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<LicensePlateDetection> rows = decision == null
                ? detections.findAllByOrderByCreatedAtDesc(page)
                : detections.findByDecisionOrderByCreatedAtDesc(decision, page);
        return rows.stream().map(DetectionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DetectionDto> pendingReviews() {
        return detections.findPendingReviews().stream().map(DetectionDto::from).toList();
    }

    // ---------------------------------------------------------- manual review
    @Transactional
    public ReviewOutcomeDto approve(String managerEmail, Long detectionId, String plateInput) {
        LicensePlateDetection d = pendingDetection(detectionId);
        String plate = plateInput.trim().replaceAll("[ -]", "").toUpperCase();

        ManualReview review = new ManualReview();
        review.setDetection(d);
        review.setStatus(ReviewStatus.APPROVED);
        review.setFinalPlate(plate);
        review.setReviewedBy(managerEmail);
        reviews.save(review);          // the UNIQUE detection_id stops a second click HERE, before the gate runs

        // the manager has confirmed the plate text: run the normal gate rules for it (full confidence)
        DetectionRequest request = new DetectionRequest(plate, 1.0);
        GateResponse out = d.getDirection() == GateDirection.ENTRY ? gate.enter(request) : gate.exit(request);

        audit.log(managerEmail, "REVIEW_APPROVE", "detection", d.getId(),
                "plate " + plate + ", gate: " + out.decision() + " - " + out.reason());
        return new ReviewOutcomeDto(d.getId(), ReviewStatus.APPROVED, plate, out.decision(), out.reason());
    }

    @Transactional
    public ReviewOutcomeDto deny(String managerEmail, Long detectionId, String note) {
        LicensePlateDetection d = pendingDetection(detectionId);

        ManualReview review = new ManualReview();
        review.setDetection(d);
        review.setStatus(ReviewStatus.DENIED);
        review.setNote(note == null || note.isBlank() ? null : note.trim());
        review.setReviewedBy(managerEmail);
        reviews.save(review);

        audit.log(managerEmail, "REVIEW_DENY", "detection", d.getId(), "dismissed without opening the gate");
        return new ReviewOutcomeDto(d.getId(), ReviewStatus.DENIED, null, null, null);
    }

    private LicensePlateDetection pendingDetection(Long id) {
        LicensePlateDetection d = detections.findById(id)
                .orElseThrow(() -> new NotFoundException("Detection " + id + " not found"));
        if (!d.isNeedsManualReview()) {
            throw new ConflictException("This detection does not need a review");
        }
        if (reviews.existsByDetectionId(id)) {
            throw new ConflictException("This detection was already reviewed");
        }
        return d;
    }
}