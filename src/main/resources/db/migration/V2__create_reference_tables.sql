CREATE TABLE instrument
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(64)  NOT NULL,
    name       VARCHAR(128) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_instrument_code UNIQUE (code)
);

CREATE INDEX ix_instrument_active ON instrument (is_active);

CREATE TABLE measurement_definition
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    unit        VARCHAR(32)  NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_measurement_definition_code UNIQUE (code)
);

CREATE INDEX ix_measurement_definition_active ON measurement_definition (is_active);

CREATE TABLE instrument_measurement
(
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instrument_id             BIGINT      NOT NULL,
    measurement_definition_id BIGINT      NOT NULL,
    display_order             INT         NOT NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_instrument_measurement_instrument
        FOREIGN KEY (instrument_id) REFERENCES instrument (id),

    CONSTRAINT fk_instrument_measurement_definition
        FOREIGN KEY (measurement_definition_id) REFERENCES measurement_definition (id),

    CONSTRAINT uq_instr_meas_instrument_definition
        UNIQUE (instrument_id, measurement_definition_id),

    CONSTRAINT uq_instr_meas_instrument_display_order
        UNIQUE (instrument_id, display_order)
);

CREATE INDEX ix_instrument_measurement_instrument
    ON instrument_measurement (instrument_id);

CREATE INDEX ix_instrument_measurement_definition
    ON instrument_measurement (measurement_definition_id);
