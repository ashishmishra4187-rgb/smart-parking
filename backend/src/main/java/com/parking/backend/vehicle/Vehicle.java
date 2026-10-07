package com.parking.backend.vehicle;
import com.parking.backend.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "vehicle")
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleType type = VehicleType.CAR;
    @Column(name = "is_primary", nullable = false)
    private boolean primaryVehicle = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public boolean isPrimaryVehicle() {
        return primaryVehicle;
    }

    public void setPrimaryVehicle(boolean primaryVehicle) {
        this.primaryVehicle = primaryVehicle;
    }
}