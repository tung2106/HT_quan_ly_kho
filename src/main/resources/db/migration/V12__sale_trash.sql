ALTER TABLE outbound_order
    ADD COLUMN trashed boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT ck_outbound_trash CHECK (NOT trashed OR (kind = 'SALE' AND paid_amount = 0));
