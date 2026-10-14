package com.parking.backend.vehicle;

import com.parking.backend.common.ConflictException;
import com.parking.backend.common.NotFoundException;
import com.parking.backend.user.User;
import com.parking.backend.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private static final String DEMO_EMAIL = "demo@parking.local"; // temporary until login exists

    private final VehicleRepository vehicles;
    private final UserRepository users;

    public VehicleService(VehicleRepository vehicles, UserRepository users) {
        this.vehicles = vehicles;
        this.users = users;
    }

    @Transactional
    public VehicleDto create(CreateVehicleRequest req) {
        String plate = req.plateNumber().replaceAll("[ -]", "").toUpperCase();
        if (vehicles.existsByPlateNumber(plate)) {
            throw new ConflictException("Vehicle " + plate + " is already registered");
        }
        User owner = users.findByEmail(DEMO_EMAIL)
                .orElseThrow(() -> new NotFoundException("Demo user missing"));
        Vehicle v = new Vehicle();
        v.setUser(owner);
        v.setPlateNumber(plate);
        v.setType(req.type());
        return toDto(vehicles.save(v));
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> list() {
        return vehicles.findAll().stream().map(this::toDto).toList();
    }

    private VehicleDto toDto(Vehicle v) {
        return new VehicleDto(v.getId(), v.getPlateNumber(), v.getType(), v.isPrimaryVehicle());
    }
}