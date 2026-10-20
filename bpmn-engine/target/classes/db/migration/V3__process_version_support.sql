-- ============================================================
-- V3__process_version_support.sql
-- Adds versioning metadata to process_definition:
--   - deployed_by      who deployed it
--   - deploy_message   optional changelog / release note
--   - active           whether this version is the "active" one
--                      (used when starting by key without explicit version)
-- ============================================================

ALTER TABLE process_definition ADD COLUMN IF NOT EXISTS deployed_by   VARCHAR(255);
ALTER TABLE process_definition ADD COLUMN IF NOT EXISTS deploy_message TEXT;
ALTER TABLE process_definition ADD COLUMN IF NOT EXISTS active         BOOLEAN NOT NULL DEFAULT TRUE;

-- For every process key, mark only the highest version as active
-- (all existing rows default to TRUE, so we clear all but the latest)
UPDATE process_definition pd
SET    active = FALSE
WHERE  version < (
           SELECT MAX(pd2.version)
           FROM   process_definition pd2
           WHERE  pd2.process_key = pd.process_key
       );

-- Ensure queries for the latest active version are fast
CREATE INDEX IF NOT EXISTS idx_pd_key_active   ON process_definition(process_key, active);
CREATE INDEX IF NOT EXISTS idx_pd_key_version  ON process_definition(process_key, version);
CREATE INDEX IF NOT EXISTS idx_pd_suspended    ON process_definition(suspended);
