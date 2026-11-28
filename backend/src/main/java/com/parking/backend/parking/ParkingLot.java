package com.parking.backend.parking;
import jakarta.persistence.*;
@Entity
@Table(name = "parking_lot")
public class ParkingLot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 200)
    private String address;
    @Column(name = "entrance_x") private double entranceX;
    @Column(name = "entrance_y") private double entranceY;
    @Column(name = "reservation_minutes", nullable = false)
    private int reservationMinutes = 10;

    @Column(columnDefinition = "text")
    private String blueprintJson;
    public String getBlueprintJson() { return blueprintJson; }
    public void setBlueprintJson(String value) { blueprintJson = value; }

    private Double gateLatitude;
    private Double gateLongitude;

    public Double getGateLatitude() { return gateLatitude; }
    public Double getGateLongitude() { return gateLongitude; }
    public void setGateLocation(double latitude, double longitude) {
        gateLatitude = latitude;
        gateLongitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getEntranceX() {
        return entranceX;
    }

    public void setEntranceX(double entranceX) {
        this.entranceX = entranceX;
    }

    public double getEntranceY() {
        return entranceY;
    }

    public void setEntranceY(double entranceY) {
        this.entranceY = entranceY;
    }

    public int getReservationMinutes() {
        return reservationMinutes;
    }

    public void setReservationMinutes(int reservationMinutes) {
        this.reservationMinutes = reservationMinutes;
    }
}