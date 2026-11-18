-- Mỗi phiếu nhập mới là một mặt hàng/một lô; giữ nguyên các dòng của phiếu cũ.
CREATE SEQUENCE purchase_lot_code_seq;
ALTER TABLE purchase_order ADD COLUMN lot_code VARCHAR(50);
ALTER TABLE purchase_order ADD COLUMN request_key UUID;
UPDATE purchase_order SET lot_code = 'NH-' || LPAD(id::text, GREATEST(6, LENGTH(id::text)), '0');
SELECT setval('purchase_lot_code_seq', COALESCE((SELECT MAX(id) FROM purchase_order), 0) + 1, false);
ALTER TABLE purchase_order ADD CONSTRAINT uq_purchase_lot_code UNIQUE (lot_code);
ALTER TABLE purchase_order ADD CONSTRAINT uq_purchase_request_key UNIQUE (request_key);
