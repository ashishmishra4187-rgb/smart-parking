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

    private final VehicleRepository vehicles;
    private final UserRepository users;

    public VehicleService(VehicleRepository vehicles, UserRepository users) {
        this.vehicles = vehicles;
        this.users = users;
    }

    @Transactional
    public VehicleDto create(String ownerEmail, CreateVehicleRequest req) {
        String plate = req.plateNumber().replaceAll("[ -]", "").toUpperCase();
        if (vehicles.existsByPlateNumber(plate)) {
            throw new ConflictException("Vehicle " + plate + " is already registered");
        }
        User owner = users.findByEmail(ownerEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Vehicle v = new Vehicle();
        v.setUser(owner);
        v.setPlateNumber(plate);
        v.setType(req.type());
        v.setPrimaryVehicle(vehicles.findByUserEmail(ownerEmail).isEmpty()); // first one is primary
        return toDto(vehicles.save(v));
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> list(String ownerEmail) {
        return vehicles.findByUserEmail(ownerEmail).stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(String ownerEmail, Long id) {
        Vehicle v = vehicles.findByIdAndUserEmail(id, ownerEmail)
                .orElseThrow(() -> new NotFoundException("Vehicle " + id + " not found"));
        vehicles.delete(v);
    }

    private VehicleDto toDto(Vehicle v) {
        return new VehicleDto(v.getId(), v.getPlateNumber(), v.getType(), v.isPrimaryVehicle());
    }
}