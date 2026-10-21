-- Hệ thống quản lý vật liệu xây dựng & nông sản (PostgreSQL)
-- Bước 1: sản phẩm, đơn vị, nhập kho, lô hàng, giao dịch kho
-- Quy ước: tiền dùng NUMERIC(15,2); số lượng dùng NUMERIC(15,3); kho luôn lưu theo ĐƠN VỊ GỐC của sản phẩm.

CREATE TABLE app_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,          -- lưu hash (BCrypt), không lưu mật khẩu thô
    role        VARCHAR(20) NOT NULL DEFAULT 'STAFF',  -- OWNER | STAFF (OWNER xem được giá vốn, lợi nhuận)
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE category (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) UNIQUE NOT NULL,       -- Xi măng, Gạch, Cát, Sắn, Ngô, Củ quả...
    perishable BOOLEAN NOT NULL DEFAULT FALSE   -- TRUE với nông sản (bật cảnh báo tồn lâu, hao hụt)
);

CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES category(id),
    code            VARCHAR(50) UNIQUE,
    name            VARCHAR(200) NOT NULL,
    base_unit       VARCHAR(20) NOT NULL,       -- đơn vị gốc: kg, viên, bao, m3...
    min_stock       NUMERIC(15,3) NOT NULL DEFAULT 0,   -- mức tồn tối thiểu để cảnh báo
    avg_cost        NUMERIC(15,2) NOT NULL DEFAULT 0,   -- giá vốn bình quân hiện tại (theo đơn vị gốc)
    retail_price    NUMERIC(15,2) NOT NULL DEFAULT 0,   -- giá lẻ theo đơn vị gốc
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    note            TEXT
);

-- Thuộc tính tùy chọn: quy cách, kích thước, độ ẩm, nguồn gốc...
CREATE TABLE product_attribute (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    attr_name   VARCHAR(100) NOT NULL,
    attr_value  VARCHAR(255) NOT NULL
);

-- Quy đổi đơn vị: 1 [unit_name] = [factor] [đơn vị gốc]. Ví dụ: 1 bao = 50 kg; 1 tạ = 100 kg
CREATE TABLE unit_conversion (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    unit_name   VARCHAR(20) NOT NULL,
    factor      NUMERIC(15,6) NOT NULL CHECK (factor > 0),
    UNIQUE (product_id, unit_name)
);

-- Giá sỉ theo bậc số lượng (tính theo đơn vị gốc). Dưới bậc thấp nhất => dùng retail_price
CREATE TABLE price_tier (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    min_qty     NUMERIC(15,3) NOT NULL CHECK (min_qty > 0),
    price       NUMERIC(15,2) NOT NULL,
    UNIQUE (product_id, min_qty)
);

CREATE TABLE supplier (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(30),
    note    TEXT
);

CREATE TABLE purchase_order (
    id            BIGSERIAL PRIMARY KEY,
    supplier_id   BIGINT REFERENCES supplier(id),
    order_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    shipping_cost NUMERIC(15,2) NOT NULL DEFAULT 0,   -- cước vận chuyển cả đơn, phân bổ vào giá vốn
    paid_amount   NUMERIC(15,2) NOT NULL DEFAULT 0,   -- đã trả, phần còn lại là công nợ
    created_by    BIGINT REFERENCES app_user(id),
    note          TEXT
);

CREATE TABLE purchase_item (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES purchase_order(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES product(id),
    qty         NUMERIC(15,3) NOT NULL CHECK (qty > 0),     -- theo đơn vị gốc (app tự quy đổi khi nhập)
    unit_cost   NUMERIC(15,2) NOT NULL CHECK (unit_cost >= 0)
);

-- Lô hàng: mỗi lần nhập một sản phẩm tạo một lô. Dùng cho FIFO và hàng tươi.
CREATE TABLE batch (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT NOT NULL REFERENCES product(id),
    purchase_item_id BIGINT REFERENCES purchase_item(id),
    received_date DATE NOT NULL DEFAULT CURRENT_DATE,
    qty_in        NUMERIC(15,3) NOT NULL,
    qty_left      NUMERIC(15,3) NOT NULL CHECK (qty_left >= 0),
    unit_cost     NUMERIC(15,2) NOT NULL        -- đã gồm phân bổ vận chuyển
);
CREATE INDEX idx_batch_fifo ON batch(product_id, received_date) WHERE qty_left > 0;

-- Mọi biến động kho. Tồn kho = tổng qty_change; KHÔNG sửa trực tiếp.
CREATE TABLE stock_transaction (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES product(id),
    batch_id    BIGINT REFERENCES batch(id),
    tx_type     VARCHAR(20) NOT NULL CHECK (tx_type IN
                  ('PURCHASE','SALE','SALE_RETURN','WASTE','ADJUSTMENT')),
    qty_change  NUMERIC(15,3) NOT NULL,         -- dương = nhập, âm = xuất
    unit_cost   NUMERIC(15,2) NOT NULL,         -- giá vốn tại thời điểm giao dịch
    ref_id      BIGINT,                         -- id đơn nhập/bán liên quan
    tx_time     TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by  BIGINT REFERENCES app_user(id),
    note        TEXT
);
CREATE INDEX idx_stock_tx_product ON stock_transaction(product_id, tx_time);

-- Tồn kho hiện tại (tính từ lịch sử giao dịch)
CREATE VIEW v_stock AS
SELECT p.id AS product_id, p.name, p.base_unit,
       COALESCE(SUM(t.qty_change), 0) AS qty_on_hand,
       p.min_stock,
       COALESCE(SUM(t.qty_change), 0) < p.min_stock AS low_stock
FROM product p
LEFT JOIN stock_transaction t ON t.product_id = p.id
GROUP BY p.id;
