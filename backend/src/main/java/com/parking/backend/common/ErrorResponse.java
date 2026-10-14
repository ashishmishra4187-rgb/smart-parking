package com.parking.backend.common;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message) {
}