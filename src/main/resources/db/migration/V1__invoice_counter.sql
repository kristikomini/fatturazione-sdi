-- Per-year invoice number counter. Schema owned by Flyway (standards rule 1); JPA validates only.
-- One row per year; the unique PK on (year) is what ON CONFLICT (year) DO NOTHING keys off, and
-- the row that SELECT ... FOR UPDATE locks during increment.
CREATE TABLE invoice_counter (
    year        INTEGER NOT NULL PRIMARY KEY,
    last_number INTEGER NOT NULL DEFAULT 0,
    version     BIGINT  NOT NULL DEFAULT 0
);
