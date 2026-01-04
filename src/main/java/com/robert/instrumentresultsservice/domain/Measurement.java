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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "measurement",
        indexes = {
                @Index(name = "ix_measurement_run", columnList = "instrument_run_id"),
                @Index(name = "ix_measurement_definition", columnList = "measurement_definition_id")
        }
)
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Run this measurement belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_run_id", nullable = false, updatable = false)
    private InstrumentRun instrumentRun;

    /**
     * Definition this measurement corresponds to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "measurement_definition_id", nullable = false, updatable = false)
    private MeasurementDefinition measurementDefinition;

    /**
     * Measured numeric value.
     */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Measurement() {
        // JPA
    }

    // --- Getters only (immutable) ---

    public Long getId() { return id; }

    public InstrumentRun getInstrumentRun() { return instrumentRun; }

    public MeasurementDefinition getMeasurementDefinition() {
        return measurementDefinition;
    }

    public BigDecimal getValue() { return value; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    // --- Package-private setters for service layer ---

    public void setInstrumentRun(InstrumentRun instrumentRun) {
        this.instrumentRun = instrumentRun;
    }

    public void setMeasurementDefinition(MeasurementDefinition measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
