package com.parking.backend.user;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "app_user")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.DRIVER;
    @Column(name = "reservation_restricted", nullable = false)
    private boolean reservationRestricted = false;
    @Column(name = "restricted_until")
    private Instant restrictedUntil;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isReservationRestricted() {
        return reservationRestricted;
    }

    public void setReservationRestricted(boolean reservationRestricted) {
        this.reservationRestricted = reservationRestricted;
    }

    public Instant getRestrictedUntil() {
        return restrictedUntil;
    }

    public void setRestrictedUntil(Instant restrictedUntil) {
        this.restrictedUntil = restrictedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}