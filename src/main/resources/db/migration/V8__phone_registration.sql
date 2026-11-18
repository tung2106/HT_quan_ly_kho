ALTER TABLE app_user ADD COLUMN pending_approval boolean NOT NULL DEFAULT false;
ALTER TABLE app_user ADD CONSTRAINT ck_pending_staff CHECK (NOT pending_approval OR (NOT active AND role = 'STAFF'));

-- Ghi nhận cả lượt gửi chưa thành công để giữ hạn mức qua các lần khởi động lại.
CREATE TABLE registration_sms (
    id uuid PRIMARY KEY,
    phone varchar(10) NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    verification_sid varchar(34),
    attempts integer NOT NULL DEFAULT 0
);
