package com.parking.backend.gate;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "entry_exit_log")
public class EntryExitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ParkingSession session;

    @Column(nullable = false, length = 30)
    private String plate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GateDirection direction;

    @Column(length = 300)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public ParkingSession getSession() { return session; }
    public void setSession(ParkingSession session) { this.session = session; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public GateDirection getDirection() { return direction; }
    public void setDirection(GateDirection direction) { this.direction = direction; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}