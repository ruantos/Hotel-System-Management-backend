package com.hotelmanagement.entity;

import java.time.Instant;

import com.hotelmanagement.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long sessionVersion;

    @Column(unique = true, length = 64)
    private String passwordResetTokenHash;

    private Instant passwordResetExpiresAt;

    private Instant passwordResetRequestedAt;

    public User() {
    }

    public Long getId() {
        return id;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getSessionVersion() {
        return sessionVersion;
    }

    public void invalidateSessions() {
        sessionVersion++;
        passwordResetTokenHash = null;
        passwordResetExpiresAt = null;
    }

    public Instant getPasswordResetExpiresAt() {
        return passwordResetExpiresAt;
    }

    public Instant getPasswordResetRequestedAt() {
        return passwordResetRequestedAt;
    }

    public void beginPasswordReset(String tokenHash, Instant expiresAt, Instant requestedAt) {
        passwordResetTokenHash = tokenHash;
        passwordResetExpiresAt = expiresAt;
        passwordResetRequestedAt = requestedAt;
    }
}
