-- V8: Convert instrument_run.process_result and instrument_run_event.details to jsonb
-- Postgres

ALTER TABLE instrument_run
ALTER COLUMN process_result TYPE jsonb
        USING (
            CASE
                WHEN process_result IS NULL OR btrim(process_result) = '' THEN NULL
                ELSE jsonb_build_object('result', process_result)
            END
        );

ALTER TABLE instrument_run_event
ALTER COLUMN details TYPE jsonb
        USING (
            CASE
                WHEN details IS NULL OR btrim(details) = '' THEN NULL
                -- If it looks like JSON (object/array), cast it
                WHEN details ~ '^\s*[\{\[]' THEN details::jsonb
                -- Otherwise store the old string as a JSON string
                ELSE to_jsonb(details)
            END
        );
