-- V7: create measurement table
-- Postgres

CREATE TABLE measurement
(
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    instrument_run_id         BIGINT         NOT NULL,
    measurement_definition_id BIGINT         NOT NULL,

    value                     NUMERIC(19, 6) NOT NULL,

    created_at                TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_measurement_run
        FOREIGN KEY (instrument_run_id)
            REFERENCES instrument_run (id),

    CONSTRAINT fk_measurement_definition
        FOREIGN KEY (measurement_definition_id)
            REFERENCES measurement_definition (id),

    -- Prevent duplicate measurements for the same run + definition
    CONSTRAINT uq_measurement_run_definition
        UNIQUE (instrument_run_id, measurement_definition_id)
);

CREATE INDEX ix_measurement_run
    ON measurement (instrument_run_id);

CREATE INDEX ix_measurement_definition
    ON measurement (measurement_definition_id);
