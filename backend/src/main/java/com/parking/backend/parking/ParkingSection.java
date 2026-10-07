package com.parking.backend.parking;
import jakarta.persistence.*;
@Entity
@Table(name = "parking_section",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "code"}))
public class ParkingSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id")
    private ParkingLot lot;
    @Column(nullable = false, length = 5)
    private String code;
    @Column(nullable = false)
    private int capacity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ParkingLot getLot() {
        return lot;
    }

    public void setLot(ParkingLot lot) {
        this.lot = lot;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}