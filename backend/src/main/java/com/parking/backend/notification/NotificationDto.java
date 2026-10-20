package com.parking.backend.notification;

import java.time.Instant;

public record NotificationDto(Long id, String type, String message, boolean read, Instant createdAt) {
}