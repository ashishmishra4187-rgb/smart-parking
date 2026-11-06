package com.parking.backend.manager;

import com.parking.backend.gate.GateDecision;
import com.parking.backend.gate.GateDirection;
import com.parking.backend.gate.LicensePlateDetection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ManagerDtos {

    private ManagerDtos() { }

    public record OverviewDto(
            long totalSlots, long available, long reserved, long occupied, long maintenance,
            long activeReservations, long carsInside,
            long entriesLast24h, long deniedLast24h, long retriesLast24h,
            long pendingReviews, Instant generatedAt) { }

    public record ActiveSessionDto(Long id, String plate, String slotCode, Instant entryTime, long minutesInside) { }

    public record ActiveReservationDto(Long id, String publicCode, String slotCode, String plate, Instant expiresAt) { }

    public record DetectionDto(Long id, String plateRaw, String plateClean, double confidence,
                               GateDirection direction, GateDecision decision, String reason,
                               boolean needsManualReview, Instant createdAt) {
        public static DetectionDto from(LicensePlateDetection d) {
            return new DetectionDto(d.getId(), d.getPlateRaw(), d.getPlateClean(), d.getConfidence(),
                    d.getDirection(), d.getDecision(), d.getReason(), d.isNeedsManualReview(), d.getCreatedAt());
        }
    }

    public record ApproveRequest(
            @NotBlank
            @Pattern(regexp = "^[A-Za-z]{2}[ -]?\\d{2}[ -]?[A-Za-z]{1,3}[ -]?\\d{4}$",
                    message = "must look like DL01AB1234")
            String plate) { }

    public record DenyRequest(@Size(max = 300) String note) { }

    public record ReviewOutcomeDto(Long detectionId, ReviewStatus status, String plate,
                                   GateDecision gateDecision, String gateReason) { }
}