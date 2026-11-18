-- Giữ dữ liệu hạn mức và các phiên cũ khi cập nhật nhà cung cấp.
ALTER TABLE registration_sms ADD COLUMN firebase_session_info text;
