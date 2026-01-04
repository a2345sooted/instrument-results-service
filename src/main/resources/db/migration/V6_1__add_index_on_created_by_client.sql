-- V6.1: add index to support querying runs by creator
-- Postgres

CREATE INDEX ix_instrument_run_created_by_client_created_at
    ON instrument_run (created_by_client_id, created_at DESC);
