-- Flyway runs this inside the "accounts" schema (spring.flyway.schemas).
-- E-mails are stored normalized (trim + lowercase) by AccountService, so a plain
-- UNIQUE constraint is enough to block duplicates.
CREATE TABLE accounts (
    id            VARCHAR(36)              PRIMARY KEY,
    name          VARCHAR(256)             NOT NULL,
    email         VARCHAR(256)             NOT NULL UNIQUE,
    password_hash VARCHAR(100)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);
