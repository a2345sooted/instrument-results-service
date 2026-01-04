-- V5: create instrument_run and instrument_run_event tables
-- Postgres

CREATE TABLE instrument_run
(
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instrument_id             BIGINT      NOT NULL,
    status                    VARCHAR(32) NOT NULL,
    measurements_submitted_at TIMESTAMPTZ NULL,
    processing_started_at     TIMESTAMPTZ NULL,
    processing_completed_at   TIMESTAMPTZ NULL,
    error_code                VARCHAR(64) NULL,
    error_message             TEXT NULL,
    process_result            TEXT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_instrument_run_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument (id)
);

CREATE INDEX ix_instrument_run_instrument
    ON instrument_run (instrument_id);

CREATE INDEX ix_instrument_run_status
    ON instrument_run (status);

CREATE INDEX ix_instrument_run_created_at
    ON instrument_run (created_at);


CREATE TABLE instrument_run_event
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instrument_run_id BIGINT      NOT NULL,
    event_type        VARCHAR(48) NOT NULL,
    details           TEXT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_instrument_run_event_run
        FOREIGN KEY (instrument_run_id) REFERENCES instrument_run (id)
);

CREATE INDEX ix_instrument_run_event_run
    ON instrument_run_event (instrument_run_id);

CREATE INDEX ix_instrument_run_event_type
    ON instrument_run_event (event_type);

CREATE INDEX ix_instrument_run_event_created_at
    ON instrument_run_event (created_at);
