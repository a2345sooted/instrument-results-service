-- V6: add client audit + external reference fields to instrument_run
-- Postgres

-- 1) Add columns (start with created_by_client_id nullable so we can backfill)
ALTER TABLE instrument_run
    ADD COLUMN created_by_client_id UUID,
  ADD COLUMN external_reference VARCHAR(128),
  ADD COLUMN measurements_submitted_by_client_id UUID;

-- 2) Backfill any existing rows so we can enforce NOT NULL.
-- Use a clearly-recognizable placeholder UUID for "unknown legacy".
UPDATE instrument_run
SET created_by_client_id = '00000000-0000-0000-0000-000000000000'
WHERE created_by_client_id IS NULL;

-- 3) Enforce NOT NULL going forward
ALTER TABLE instrument_run
    ALTER COLUMN created_by_client_id SET NOT NULL;

-- 4) Index external_reference for lookups/correlation (optional but useful)
CREATE INDEX ix_instrument_run_external_ref
    ON instrument_run (external_reference);
