ALTER TABLE registration_sms ADD COLUMN consent_version integer;

ALTER TABLE app_user
    ADD COLUMN registration_consent_version integer,
    ADD COLUMN registration_consented_at timestamptz;
