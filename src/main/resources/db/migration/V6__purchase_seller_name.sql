-- Ghi tên người bán ngay trên phiếu; giữ liên kết nhà cung cấp của dữ liệu cũ.
ALTER TABLE purchase_order ADD COLUMN seller_name VARCHAR(150);
