package com.parking.backend.parking;

import com.parking.backend.common.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class GateLocationController {
    private final ParkingLotRepository lots;
    public GateLocationController(ParkingLotRepository lots) { this.lots = lots; }

    public record Pin(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {}

    @Transactional
    @PutMapping("/api/manager/lots/{id}/entrance")
    public Pin save(@PathVariable long id, @Valid @RequestBody Pin pin) {
        var lot = lots.findById(id).orElseThrow(() -> new NotFoundException("Lot not found"));
        lot.setGateLocation(pin.latitude(), pin.longitude());
        return pin;
    }
}
