package com.parking.backend.parking;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking/lots")
public class ParkingController {

    private final ParkingService service;

    public ParkingController(ParkingService service) {
        this.service = service;
    }

    @GetMapping
    public List<LotDto> lots() {
        return service.getLots();
    }

    @GetMapping("/{id}/slots")
    public List<SlotDto> slots(@PathVariable Long id) {
        return service.getSlots(id);
    }
}