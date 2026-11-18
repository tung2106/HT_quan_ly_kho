-- Giữ người lập trong lịch sử kho, kể cả khi tài khoản đã bị xóa.
ALTER TABLE app_user ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE app_user ADD CONSTRAINT ck_deleted_staff_inactive
    CHECK (deleted_at IS NULL OR (role = 'STAFF' AND NOT active));
