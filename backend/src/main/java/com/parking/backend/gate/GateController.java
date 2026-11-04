package com.parking.backend.gate;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/detection")
public class GateController {

    private final GateService service;

    public GateController(GateService service) {
        this.service = service;
    }

    @PostMapping("/entry")
    public GateResponse entry(@Valid @RequestBody DetectionRequest request) {
        return service.enter(request);
    }

    @PostMapping("/exit")
    public GateResponse exit(@Valid @RequestBody DetectionRequest request) {
        return service.exit(request);
    }
}