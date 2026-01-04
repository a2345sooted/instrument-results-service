package com.robert.instrumentresultsservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "instrument_run",
        indexes = {
                @Index(name = "ix_instrument_run_instrument", columnList = "instrument_id"),
                @Index(name = "ix_instrument_run_status", columnList = "status"),
                @Index(name = "ix_instrument_run_created_at", columnList = "created_at"),
                @Index(name = "ix_instrument_run_external_ref", columnList = "external_reference")
        }
)
public class InstrumentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Instrument model/config this run was executed against.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false, updatable = false)
    private Instrument instrument;

    /**
     * Who created this run.
     */
    @Column(name = "created_by_client_id", nullable = false, updatable = false)
    private UUID createdByClientId;

    /**
     * Optional external reference (idempotency key, correlation id, etc).
     */
    @Column(name = "external_reference", length = 128)
    private String externalReference;

    /**
     * Current lifecycle state.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InstrumentRunStatus status = InstrumentRunStatus.CREATED;

    /**
     * Domain timestamps.
     */
    @Column(name = "measurements_submitted_at")
    private OffsetDateTime measurementsSubmittedAt;

    @Column(name = "measurements_submitted_by_client_id")
    private UUID measurementsSubmittedByClientId;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private OffsetDateTime processingCompletedAt;

    /**
     * Error fields when FAILED.
     */
    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Processing result payload (TEXT now, jsonb later).
     */
    @Column(name = "process_result", columnDefinition = "TEXT")
    private String processResult;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public InstrumentRun() {
        // JPA
    }

    // --- Getters & setters ---

    public Long getId() { return id; }

    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }

    public UUID getCreatedByClientId() { return createdByClientId; }
    public void setCreatedByClientId(UUID createdByClientId) {
        this.createdByClientId = createdByClientId;
    }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public InstrumentRunStatus getStatus() { return status; }
    public void setStatus(InstrumentRunStatus status) { this.status = status; }

    public OffsetDateTime getMeasurementsSubmittedAt() { return measurementsSubmittedAt; }
    public void setMeasurementsSubmittedAt(OffsetDateTime measurementsSubmittedAt) {
        this.measurementsSubmittedAt = measurementsSubmittedAt;
    }

    public UUID getMeasurementsSubmittedByClientId() {
        return measurementsSubmittedByClientId;
    }

    public void setMeasurementsSubmittedByClientId(UUID measurementsSubmittedByClientId) {
        this.measurementsSubmittedByClientId = measurementsSubmittedByClientId;
    }

    public OffsetDateTime getProcessingStartedAt() { return processingStartedAt; }
    public void setProcessingStartedAt(OffsetDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public OffsetDateTime getProcessingCompletedAt() { return processingCompletedAt; }
    public void setProcessingCompletedAt(OffsetDateTime processingCompletedAt) {
        this.processingCompletedAt = processingCompletedAt;
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getProcessResult() { return processResult; }
    public void setProcessResult(String processResult) { this.processResult = processResult; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
