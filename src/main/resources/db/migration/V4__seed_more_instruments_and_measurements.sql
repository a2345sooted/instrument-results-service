-- V4 seed: add additional instruments, measurement definitions, and mappings.
-- Postgres.

-- 1) Instruments
INSERT INTO instrument (code, name, is_active)
VALUES
    ('INSTR_DEMO_002', 'Demo Instrument 002', TRUE),
    ('INSTR_DEMO_003', 'Demo Instrument 003', TRUE)
    ON CONFLICT (code) DO NOTHING;

-- 2) Measurement definitions
INSERT INTO measurement_definition (code, name, unit, description, is_active)
VALUES
    ('meas_demo_c', 'Demo Measurement C', 'unitC', 'Demo measurement C description', TRUE),
    ('meas_demo_d', 'Demo Measurement D', 'unitD', 'Demo measurement D description', TRUE),
    ('meas_demo_e', 'Demo Measurement E', 'unitE', 'Demo measurement E description', TRUE)
    ON CONFLICT (code) DO NOTHING;

-- 3) Contract mappings (instrument_measurement)
-- Demo Instrument 002 -> C, D
WITH i AS (
    SELECT id FROM instrument WHERE code = 'INSTR_DEMO_002'
),
     d AS (
         SELECT id, code FROM measurement_definition WHERE code IN ('meas_demo_c', 'meas_demo_d')
     )
INSERT INTO instrument_measurement (instrument_id, measurement_definition_id, display_order)
SELECT
    (SELECT id FROM i),
    d.id,
    CASE d.code
        WHEN 'meas_demo_c' THEN 1
        WHEN 'meas_demo_d' THEN 2
        END AS display_order
FROM d
    ON CONFLICT (instrument_id, measurement_definition_id) DO NOTHING;

-- Demo Instrument 003 -> D, E
WITH i AS (
    SELECT id FROM instrument WHERE code = 'INSTR_DEMO_003'
),
     d AS (
         SELECT id, code FROM measurement_definition WHERE code IN ('meas_demo_d', 'meas_demo_e')
     )
INSERT INTO instrument_measurement (instrument_id, measurement_definition_id, display_order)
SELECT
    (SELECT id FROM i),
    d.id,
    CASE d.code
        WHEN 'meas_demo_d' THEN 1
        WHEN 'meas_demo_e' THEN 2
        END AS display_order
FROM d
    ON CONFLICT (instrument_id, measurement_definition_id) DO NOTHING;
