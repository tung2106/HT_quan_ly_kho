CREATE TABLE login_rate_limit (
    bucket VARCHAR(64) PRIMARY KEY,
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX login_rate_limit_expiry ON login_rate_limit (expires_at);
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'kho_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON login_rate_limit TO kho_app;
    END IF;
END $$;
