package com.parking.backend.manager;

import com.parking.backend.gate.GateDecision;
import com.parking.backend.manager.ManagerDtos.ActiveReservationDto;
import com.parking.backend.manager.ManagerDtos.ActiveSessionDto;
import com.parking.backend.manager.ManagerDtos.ApproveRequest;
import com.parking.backend.manager.ManagerDtos.DenyRequest;
import com.parking.backend.manager.ManagerDtos.DetectionDto;
import com.parking.backend.manager.ManagerDtos.OverviewDto;
import com.parking.backend.manager.ManagerDtos.ReviewOutcomeDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class ManagerDashboardController {

    private final ManagerService service;

    public ManagerDashboardController(ManagerService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public OverviewDto overview() {
        return service.overview();
    }

    @GetMapping("/sessions/active")
    public List<ActiveSessionDto> carsInside() {
        return service.carsInside();
    }

    @GetMapping("/reservations/active")
    public List<ActiveReservationDto> activeReservations() {
        return service.activeReservations();
    }

    @GetMapping("/detections")
    public List<DetectionDto> detections(@RequestParam(required = false) GateDecision decision,
                                         @RequestParam(defaultValue = "50") int limit) {
        return service.recentDetections(decision, Math.max(1, Math.min(limit, 200)));
    }

    @GetMapping("/reviews")
    public List<DetectionDto> pendingReviews() {
        return service.pendingReviews();
    }

    @PostMapping("/reviews/{id}/approve")
    public ReviewOutcomeDto approve(Authentication auth, @PathVariable Long id,
                                    @Valid @RequestBody ApproveRequest request) {
        return service.approve(auth.getName(), id, request.plate());
    }

    @PostMapping("/reviews/{id}/deny")
    public ReviewOutcomeDto deny(Authentication auth, @PathVariable Long id,
                                 @Valid @RequestBody DenyRequest request) {
        return service.deny(auth.getName(), id, request.note());
    }
}