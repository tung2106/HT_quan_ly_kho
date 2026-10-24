-- Hai nhóm bán hàng độc lập với danh mục cũ; không đổi đơn vị hoặc giao dịch đã ghi.
ALTER TABLE product ADD COLUMN product_group VARCHAR(20) NOT NULL DEFAULT 'MATERIAL';
UPDATE product p SET product_group='AGRICULTURE'
FROM category c WHERE c.id=p.category_id AND p.base_unit='kg'
AND (c.perishable OR c.name IN ('Sắn','Ngô','Sắn khô','khoai tây','Khoai tây','Nông sản'));
ALTER TABLE product ADD CONSTRAINT product_group_check CHECK (product_group IN ('MATERIAL','AGRICULTURE'));
ALTER TABLE product ALTER COLUMN category_id DROP NOT NULL;

ALTER TABLE purchase_item ADD COLUMN entry_mode VARCHAR(20) NOT NULL DEFAULT 'WEIGHT';
ALTER TABLE purchase_item ADD COLUMN bag_weights TEXT;
ALTER TABLE purchase_item ADD COLUMN line_amount NUMERIC(24,5);
UPDATE purchase_item SET line_amount=qty*unit_cost;
ALTER TABLE purchase_item ALTER COLUMN line_amount SET NOT NULL;
ALTER TABLE purchase_item ADD CONSTRAINT purchase_entry_mode_check CHECK (entry_mode IN ('WEIGHT','BAGS','LOT'));
-- Lưu giá/kg tính từ tổng tiền lô với độ chính xác cao hơn, không làm tròn về đồng/kg.
ALTER TABLE purchase_item ALTER COLUMN unit_cost TYPE NUMERIC(25,12);
ALTER TABLE batch ALTER COLUMN unit_cost TYPE NUMERIC(25,12);
ALTER TABLE stock_transaction ALTER COLUMN unit_cost TYPE NUMERIC(25,12);
