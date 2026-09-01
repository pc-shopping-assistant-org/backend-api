-- =====================================================================
-- Google Identity Services account link
-- =====================================================================
-- Local registration remains the source of the required phone/password and
-- profile data. Google Login links a verified provider subject to that
-- existing account; NULL is retained for accounts that have not linked Google.

ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS google_subject varchar(255);

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_google_subject
    ON accounts (google_subject)
    WHERE google_subject IS NOT NULL;
