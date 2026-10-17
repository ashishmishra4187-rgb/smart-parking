package com.parking.backend.reservation;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto reserve(Authentication auth, @Valid @RequestBody CreateReservationRequest request) {
        return service.reserve(auth.getName(), request);
    }

    @GetMapping("/mine")
    public List<ReservationDto> mine(Authentication auth) {
        return service.mine(auth.getName());
    }

    @GetMapping("/{id}")
    public ReservationDto get(Authentication auth, @PathVariable Long id) {
        return service.get(auth.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(Authentication auth, @PathVariable Long id) {
        service.cancel(auth.getName(), id);
    }
}