-- V3 seed: one instrument, two measurement defs, and contract mapping.
-- Postgres.

WITH ins_instrument AS (
INSERT INTO instrument (code, name, is_active)
VALUES ('INSTR_DEMO_001', 'Demo Instrument 001', TRUE)
    RETURNING id
    ),
    ins_defs AS (
INSERT INTO measurement_definition (code, name, unit, description, is_active)
VALUES
    ('meas_demo_a', 'Demo Measurement A', 'unitA', 'Demo measurement A description', TRUE),
    ('meas_demo_b', 'Demo Measurement B', 'unitB', 'Demo measurement B description', TRUE)
    RETURNING id, code
    )
INSERT INTO instrument_measurement (instrument_id, measurement_definition_id, display_order)
SELECT
    (SELECT id FROM ins_instrument),
    d.id,
    CASE d.code
        WHEN 'meas_demo_a' THEN 1
        WHEN 'meas_demo_b' THEN 2
        END AS display_order
FROM ins_defs d
ORDER BY display_order;
