package com.robert.instrumentresultsservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Contract row describing which measurements an Instrument produces.
 *
 * Reference data:
 * - Created/maintained via DB migrations
 * - Runtime app treats as read-only
 */
@Entity
@Table(
        name = "instrument_measurement",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_instrument_measurement_instrument_definition",
                        columnNames = {"instrument_id", "measurement_definition_id"}
                ),
                @UniqueConstraint(
                        name = "uq_instrument_measurement_instrument_display_order",
                        columnNames = {"instrument_id", "display_order"}
                )
        },
        indexes = {
                @Index(name = "ix_instrument_measurement_instrument", columnList = "instrument_id"),
                @Index(name = "ix_instrument_measurement_definition", columnList = "measurement_definition_id")
        }
)
public class InstrumentMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false, updatable = false)
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "measurement_definition_id", nullable = false, updatable = false)
    private MeasurementDefinition measurementDefinition;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected InstrumentMeasurement() {
        // JPA
    }

    // --- Getters & setters ---

    public Long getId() { return id; }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }

    public MeasurementDefinition getMeasurementDefinition() { return measurementDefinition; }
    public void setMeasurementDefinition(MeasurementDefinition measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
