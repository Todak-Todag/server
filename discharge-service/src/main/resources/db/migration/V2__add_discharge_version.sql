ALTER TABLE discharge_schema.p_discharges
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;