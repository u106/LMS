-- The bytes live in S3; the database keeps only what it can usefully answer
-- questions about. Storing the file itself here would bloat every backup
-- with data MySQL cannot query - see ADR-007.

ALTER TABLE lessons
    ADD COLUMN file_name    VARCHAR(255) NULL AFTER file_key,
    ADD COLUMN content_type VARCHAR(100) NULL AFTER file_name,
    ADD COLUMN file_size    BIGINT       NULL AFTER content_type;
