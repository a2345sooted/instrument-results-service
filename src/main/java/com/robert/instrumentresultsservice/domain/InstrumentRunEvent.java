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

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "instrument_run_event",
        indexes = {
                @Index(name = "ix_instrument_run_event_run", columnList = "instrument_run_id"),
                @Index(name = "ix_instrument_run_event_type", columnList = "event_type"),
                @Index(name = "ix_instrument_run_event_created_at", columnList = "created_at")
        }
)
public class InstrumentRunEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent run.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_run_id", nullable = false, updatable = false)
    private InstrumentRun instrumentRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private InstrumentRunEventType eventType;

    /**
     * Optional event details. Start as TEXT; can move to jsonb later.
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public InstrumentRunEvent() {
        // JPA
    }

    // --- Getters & setters ---

    public Long getId() { return id; }

    public InstrumentRun getInstrumentRun() { return instrumentRun; }
    public void setInstrumentRun(InstrumentRun instrumentRun) { this.instrumentRun = instrumentRun; }

    public InstrumentRunEventType getEventType() { return eventType; }
    public void setEventType(InstrumentRunEventType eventType) { this.eventType = eventType; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
