-- Phiếu cũ không có người lập: chỉ quản lý được xem, không tự gán cho nhân viên.
ALTER TABLE app_user ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_user ADD CONSTRAINT ck_user_role CHECK (role IN ('OWNER','STAFF'));
CREATE UNIQUE INDEX idx_user_username_lower ON app_user(lower(username));
ALTER TABLE outbound_order ADD COLUMN created_by BIGINT REFERENCES app_user(id);
ALTER TABLE cash_payment ADD COLUMN created_by BIGINT REFERENCES app_user(id);
CREATE INDEX idx_outbound_creator ON outbound_order(created_by,id);
