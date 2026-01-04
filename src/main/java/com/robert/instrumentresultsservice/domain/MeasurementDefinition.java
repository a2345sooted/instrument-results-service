package com.robert.instrumentresultsservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Defines the semantic meaning of a measurement (e.g., "ph", "temperature_c").
 * Reference data:
 * - Created and maintained via DB migrations
 * - Treated as read-only by the runtime application
 */
@Entity
@Table(
        name = "measurement_definition",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_measurement_definition_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "ix_measurement_definition_active",
                        columnList = "is_active"
                )
        }
)
public class MeasurementDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stable, human-friendly key used in migrations and APIs.
     * Examples: "ph", "temperature_c", "pressure_kpa"
     */
    @Column(nullable = false, length = 64)
    private String code;

    /**
     * Display name shown to users.
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * Unit label shown to users (e.g., "pH", "°C", "kPa").
     */
    @Column(nullable = false, length = 32)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Indicates whether this measurement definition is active.
     * Historical measurements remain valid if set to false.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public MeasurementDefinition() {
    }

    // --- Getters & setters ---

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
