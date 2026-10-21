-- Phiếu nội bộ và lịch sử thu/trả; giữ nguyên dữ liệu nhập hàng cũ.
ALTER TABLE purchase_order ADD COLUMN total_amount NUMERIC(20,0);
UPDATE purchase_order o SET total_amount = ROUND(o.shipping_cost + COALESCE(
    (SELECT SUM(i.qty * i.unit_cost) FROM purchase_item i WHERE i.order_id = o.id), 0));

CREATE TABLE outbound_order (
    id BIGSERIAL PRIMARY KEY,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('SALE','WASTE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer VARCHAR(200),
    note TEXT,
    total_amount NUMERIC(20,0) NOT NULL CHECK (total_amount >= 0),
    cost_amount NUMERIC(25,5) NOT NULL CHECK (cost_amount >= 0),
    paid_amount NUMERIC(20,0) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0 AND paid_amount <= total_amount),
    request_key UUID NOT NULL UNIQUE
);
CREATE TABLE outbound_item (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES outbound_order(id),
    product_id BIGINT NOT NULL REFERENCES product(id),
    qty NUMERIC(15,3) NOT NULL CHECK (qty > 0),
    entered_qty NUMERIC NOT NULL,
    entered_unit VARCHAR(20) NOT NULL,
    unit_price NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    line_amount NUMERIC NOT NULL,
    cost_amount NUMERIC(25,5) NOT NULL
);
CREATE INDEX idx_outbound_item_order ON outbound_item(order_id);
CREATE TABLE cash_payment (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT REFERENCES purchase_order(id),
    outbound_id BIGINT REFERENCES outbound_order(id),
    amount NUMERIC(20,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    request_key UUID NOT NULL UNIQUE,
    CHECK ((purchase_id IS NULL) <> (outbound_id IS NULL))
);
CREATE INDEX idx_payment_purchase ON cash_payment(purchase_id);
CREATE INDEX idx_payment_outbound ON cash_payment(outbound_id);
-- Khoản đã trả trước khi có sổ thu chi: không suy diễn ngày thanh toán thực tế.
INSERT INTO cash_payment(purchase_id, amount, created_at, note, request_key)
SELECT id, paid_amount, order_date::timestamp, 'Số đã trả chuyển từ phiếu nhập cũ; ngày theo phiếu nhập',
       md5('purchase-opening-' || id)::uuid
FROM purchase_order WHERE paid_amount > 0;
