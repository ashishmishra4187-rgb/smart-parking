package com.parking.backend.reservation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiryScheduler.class);

    private final ReservationRepository reservations;
    private final ReservationExpiryService expiry;

    public ExpiryScheduler(ReservationRepository reservations, ReservationExpiryService expiry) {
        this.reservations = reservations;
        this.expiry = expiry;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 30_000)
    public void run() {
        Instant now = Instant.now();

        for (Long id : reservations.findIdsToExpire(ReservationStatus.ACTIVE, now)) {
            try {
                expiry.expire(id);
                log.info("Expired reservation {}", id);
            } catch (Exception e) {
                log.warn("Could not expire reservation {}: {}", id, e.getMessage());
            }
        }

        Instant threshold = now.plus(2, ChronoUnit.MINUTES);
        for (Long id : reservations.findIdsToWarn(ReservationStatus.ACTIVE, now, threshold)) {
            try {
                expiry.warn(id);
            } catch (Exception e) {
                log.warn("Could not warn for reservation {}: {}", id, e.getMessage());
            }
        }
    }
}