package com.parking.backend.gate;

public record GateResponse(
        Long detectionId,
        GateDecision decision,
        String reason,
        String plate,
        String reservationCode,
        String slotCode,
        boolean needsManualReview) {
}