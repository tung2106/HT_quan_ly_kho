package com.vatlieu.service;

import com.vatlieu.form.OutboundForm;
import com.vatlieu.form.PurchaseItemForm;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class OperationsService {
    private final JdbcTemplate jdbc;
    private final CurrentUser current;

    private void lockRequest(UUID key) {
        if (key == null) throw new IllegalArgumentException("Thiếu mã phiếu, hãy mở lại biểu mẫu");
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", key.toString());
    }

    @Transactional
    public long create(OutboundForm form) {
        long actor=current.id();
        if(!"SALE".equals(form.getKind())) current.requireOwner();
        lockRequest(form.getRequestKey());
        List<Long> existing = jdbc.queryForList("SELECT id FROM outbound_order WHERE request_key=?", Long.class, form.getRequestKey());
        if (!existing.isEmpty()) { detail(existing.get(0));return existing.get(0); }
        boolean sale = "SALE".equals(form.getKind());
        if (!sale && !"WASTE".equals(form.getKind())) throw new IllegalArgumentException("Loại phiếu không hợp lệ");
        if (!sale && (form.getNote() == null || form.getNote().isBlank())) throw new IllegalArgumentException("Hãy ghi lý do hao hụt");
        if (form.getCustomer() != null && form.getCustomer().length() > 200) throw new IllegalArgumentException("Tên khách tối đa 200 ký tự");
        if (form.getItems() == null || form.getItems().size() > 100) throw new IllegalArgumentException("Mỗi phiếu tối đa 100 dòng");
        List<PurchaseItemForm> rows = form.getItems().stream().filter(r -> r.getProductId() != null || r.getQty() != null || r.getUnitPrice() != null).toList();
        if (rows.isEmpty()) throw new IllegalArgumentException("Cần ít nhất một dòng hàng");
        // Khóa sản phẩm theo cùng thứ tự với nhập kho để tránh bán trùng tồn và deadlock.
        for (Long id : rows.stream().map(PurchaseItemForm::getProductId).filter(Objects::nonNull).distinct().sorted().toList()) {
            if (jdbc.queryForList("SELECT id FROM product WHERE id=? AND active FOR UPDATE", id).isEmpty())
                throw new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng sử dụng");
        }
        record Line(Long batch, long product, BigDecimal qty, BigDecimal enteredQty, String unit, BigDecimal price, BigDecimal amount) {}
        List<Line> lines = new ArrayList<>();
        for (PurchaseItemForm row : rows) {
            if (row.getProductId() == null) throw new IllegalArgumentException("Hãy chọn sản phẩm cho mỗi dòng đã nhập");
            String base = jdbc.queryForObject("SELECT base_unit FROM product WHERE id=?", String.class, row.getProductId());
            String unit = row.getUnit() == null || row.getUnit().isBlank() ? base : row.getUnit().trim();
            String group = jdbc.queryForObject("SELECT product_group FROM product WHERE id=?", String.class, row.getProductId());
            if ("AGRICULTURE".equals(group) && !"kg".equals(unit))
                throw new IllegalArgumentException("Nông sản xuất theo kg cân thực tế, không quy đổi bao");
            List<BigDecimal> factors = unit.equalsIgnoreCase(base) ? List.of(BigDecimal.ONE) : jdbc.queryForList(
                    "SELECT factor FROM unit_conversion WHERE product_id=? AND lower(unit_name)=lower(?)", BigDecimal.class, row.getProductId(), unit);
            if (factors.size() != 1) throw new IllegalArgumentException("Chưa khai báo quy đổi đơn vị: " + unit);
            BigDecimal qty = InventoryMath.quantity(row.getQty(), factors.get(0));
            BigDecimal price = sale ? InventoryMath.price(row.getUnitPrice()) : BigDecimal.ZERO;
            lines.add(new Line(sale ? row.getBatchId() : null, row.getProductId(), qty, row.getQty(), unit, price, row.getQty().multiply(price)));
        }
        BigDecimal total = InventoryMath.total(lines.stream().map(Line::amount).toList());
        BigDecimal paid = InventoryMath.payment(form.getPaidAmount() == null ? BigDecimal.ZERO : form.getPaidAmount(), total);
        if (paid.compareTo(total) < 0 && (form.getCustomer() == null || form.getCustomer().isBlank()))
            throw new IllegalArgumentException("Hãy ghi tên/thông tin khách để theo dõi khoản nợ");
        Long orderId = jdbc.queryForObject("""
                INSERT INTO outbound_order(kind,customer,note,total_amount,cost_amount,paid_amount,request_key,created_by)
                VALUES (?,?,?,?,0,?,?,?) RETURNING id
                """, Long.class, form.getKind(), sale ? form.getCustomer() : null, form.getNote(), total, paid, form.getRequestKey(),actor);
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Line line : lines) {
            BigDecimal stock = jdbc.queryForObject("SELECT COALESCE(SUM(qty_change),0) FROM stock_transaction WHERE product_id=?", BigDecimal.class, line.product());
            if (stock.compareTo(line.qty()) < 0) throw new IllegalArgumentException("Không đủ tồn kho cho sản phẩm #" + line.product());
            List<InventoryMath.Lot> lots = jdbc.query("SELECT id,qty_left,unit_cost FROM batch WHERE product_id=? AND qty_left>0 ORDER BY received_date,id FOR UPDATE",
                    (rs,n) -> new InventoryMath.Lot(rs.getLong(1),rs.getBigDecimal(2),rs.getBigDecimal(3)), line.product());
            BigDecimal lotStock = lots.stream().map(InventoryMath.Lot::qty).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (stock.compareTo(lotStock) != 0) throw new IllegalArgumentException("Tồn lô lệch lịch sử kho; cần đối chiếu trước khi xuất");
            List<InventoryMath.Lot> selected = lots;
            if (line.batch() != null) {
                selected = lots.stream().filter(l -> l.id() == line.batch()).toList();
                if (selected.isEmpty()) throw new IllegalArgumentException("Lô đã hết hàng hoặc không thuộc sản phẩm đã chọn");
                if (selected.get(0).qty().compareTo(line.qty()) < 0)
                    throw new IllegalArgumentException("Số lượng vượt tồn của lô #" + line.batch());
            }
            List<InventoryMath.Take> takes = InventoryMath.fifo(line.qty(), selected);
            BigDecimal cost = InventoryMath.cost(takes);
            totalCost = totalCost.add(cost);
            jdbc.update("INSERT INTO outbound_item(order_id,product_id,qty,entered_qty,entered_unit,unit_price,line_amount,cost_amount) VALUES (?,?,?,?,?,?,?,?)",
                    orderId,line.product(),line.qty(),line.enteredQty(),line.unit(),line.price(),line.amount(),cost);
            for (InventoryMath.Take take : takes) {
                jdbc.update("UPDATE batch SET qty_left=qty_left-? WHERE id=?",take.qty(),take.id());
                jdbc.update("INSERT INTO stock_transaction(product_id,batch_id,tx_type,qty_change,unit_cost,ref_id,note,created_by) VALUES (?,?,?,?,?,?,?,?)",
                        line.product(),take.id(),form.getKind(),take.qty().negate(),take.cost(),orderId,form.getNote(),actor);
            }
            jdbc.update("""
                    UPDATE product SET avg_cost=(SELECT CASE WHEN SUM(qty_left)>0
                    THEN ROUND(SUM(qty_left*unit_cost)/SUM(qty_left),2) ELSE 0 END FROM batch WHERE product_id=?) WHERE id=?
                    """,line.product(),line.product());
        }
        jdbc.update("UPDATE outbound_order SET cost_amount=? WHERE id=?",totalCost,orderId);
        if (paid.signum()>0) jdbc.update("INSERT INTO cash_payment(outbound_id,amount,note,request_key,created_by) VALUES (?,?,?,?,?)",orderId,paid,"Thu lúc xuất bán",form.getRequestKey(),actor);
        return orderId;
    }

    @Transactional
    public void pay(String type, long id, BigDecimal amount, UUID key, String note) {
        current.requireOwner();
        lockRequest(key);
        if (!jdbc.queryForList("SELECT id FROM cash_payment WHERE request_key=?",key).isEmpty()) return;
        boolean purchase = "purchase".equals(type);
        if (!purchase && !"sale".equals(type)) throw new IllegalArgumentException("Loại thanh toán không hợp lệ");
        String table = purchase ? "purchase_order" : "outbound_order";
        List<Map<String,Object>> found = jdbc.queryForList("SELECT * FROM " + table + " WHERE id=? FOR UPDATE",id);
        if (found.isEmpty()) throw new IllegalArgumentException("Không tìm thấy phiếu");
        Map<String,Object> order = found.get(0);
        if (!purchase && !"SALE".equals(order.get("kind"))) throw new IllegalArgumentException("Phiếu hao hụt không thu tiền");
        BigDecimal remaining = ((BigDecimal)order.get("total_amount")).subtract((BigDecimal)order.get("paid_amount"));
        amount = InventoryMath.payment(amount,remaining);
        if (amount.signum() == 0) throw new IllegalArgumentException("Tiền thanh toán phải lớn hơn 0");
        jdbc.update("UPDATE " + table + " SET paid_amount=paid_amount+? WHERE id=?",amount,id);
        jdbc.update("INSERT INTO cash_payment("+(purchase ? "purchase_id" : "outbound_id")+",amount,note,request_key,created_by) VALUES (?,?,?,?,?)",id,amount,note,key,current.id());
    }

    public List<Map<String,Object>> orders() {
        if(!current.owner()) return jdbc.queryForList("SELECT id,kind,created_at,customer,note,total_amount,paid_amount FROM outbound_order WHERE kind='SALE' AND created_by=? ORDER BY id DESC",current.id());
        return jdbc.queryForList("SELECT * FROM outbound_order ORDER BY id DESC");
    }
    public Map<String,Object> detail(long id) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM outbound_order WHERE id=?",id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy phiếu");
        Map<String,Object> order = rows.get(0);
        boolean owner=current.owner();
        if(!owner && (!"SALE".equals(order.get("kind")) || !Objects.equals(order.get("created_by"),current.id())))
            throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ được xem phiếu bán do mình tạo");
        order.put("items",jdbc.queryForList("SELECT i.*,p.name,p.base_unit FROM outbound_item i JOIN product p ON p.id=i.product_id WHERE order_id=? ORDER BY i.id",id));
        var usedLots = jdbc.queryForList("SELECT t.batch_id,p.name,p.base_unit,-t.qty_change AS qty FROM stock_transaction t JOIN product p ON p.id=t.product_id WHERE t.ref_id=? AND t.tx_type=? ORDER BY t.id",id,order.get("kind"));
        order.put("usedLots", usedLots);
        if(!owner) {
            order.remove("cost_amount");
            ((List<Map<String,Object>>)order.get("items")).forEach(item -> item.remove("cost_amount"));
        }
        return order;
    }
    public List<Map<String,Object>> debts() {
        current.requireOwner();
        return jdbc.queryForList("""
                SELECT 'sale' AS type,id,COALESCE(NULLIF(TRIM(customer),''),'Khách lẻ') AS partner,total_amount,paid_amount,total_amount-paid_amount AS remaining
                FROM outbound_order WHERE kind='SALE'
                UNION ALL
                SELECT 'purchase',o.id,COALESCE(s.name,'Chưa rõ NCC'),o.total_amount,o.paid_amount,o.total_amount-o.paid_amount
                FROM purchase_order o LEFT JOIN supplier s ON s.id=o.supplier_id
                ORDER BY remaining DESC,id DESC
                """);
    }
    public List<Map<String,Object>> payments(boolean receipts, String from, String to, String partner) {
        current.requireOwner();
        java.time.LocalDate start, end;
        try {
            start = from.isBlank() ? null : java.time.LocalDate.parse(from);
            end = to.isBlank() ? null : java.time.LocalDate.parse(to);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày lọc không hợp lệ");
        }
        if (start != null && end != null && start.isAfter(end))
            throw new IllegalArgumentException("Từ ngày phải trước hoặc bằng đến ngày");
        String sql = """
                SELECT * FROM (
                SELECT p.*, CASE WHEN p.outbound_id IS NOT NULL
                    THEN COALESCE(NULLIF(TRIM(o.customer),''),'Khách lẻ')
                    ELSE COALESCE(s.name,'Chưa rõ NCC') END AS partner
                FROM cash_payment p
                LEFT JOIN outbound_order o ON o.id=p.outbound_id
                LEFT JOIN purchase_order i ON i.id=p.purchase_id
                LEFT JOIN supplier s ON s.id=i.supplier_id
                ) history WHERE
                """ + (receipts ? "outbound_id" : "purchase_id") + " IS NOT NULL";
        List<Object> args = new ArrayList<>();
        if (start != null) { sql += " AND created_at::date >= ?"; args.add(java.sql.Date.valueOf(start)); }
        if (end != null) { sql += " AND created_at::date <= ?"; args.add(java.sql.Date.valueOf(end)); }
        // Tìm chuỗi con, không coi ký tự % hay _ người dùng nhập là ký tự đại diện.
        if (!partner.isBlank()) { sql += " AND POSITION(LOWER(?) IN LOWER(partner)) > 0"; args.add(partner.trim()); }
        return jdbc.queryForList(sql + " ORDER BY created_at DESC,id DESC", args.toArray());
    }
    public Map<String,Object> summary() {
        current.requireOwner();
        return jdbc.queryForMap("""
                SELECT
                (SELECT COALESCE(SUM(qty_left*unit_cost),0) FROM batch) AS inventory,
                (SELECT COALESCE(SUM(total_amount),0) FROM outbound_order WHERE kind='SALE') AS revenue,
                (SELECT COALESCE(SUM(cost_amount),0) FROM outbound_order WHERE kind='SALE') AS sold_cost,
                (SELECT COALESCE(SUM(cost_amount),0) FROM outbound_order WHERE kind='WASTE') AS waste,
                (SELECT COALESCE(SUM(amount),0) FROM cash_payment WHERE outbound_id IS NOT NULL) AS receipts,
                (SELECT COALESCE(SUM(amount),0) FROM cash_payment WHERE purchase_id IS NOT NULL) AS payments,
                (SELECT COALESCE(SUM(total_amount-paid_amount),0) FROM outbound_order WHERE kind='SALE') AS receivable,
                (SELECT COALESCE(SUM(total_amount-paid_amount),0) FROM purchase_order) AS payable
                """);
    }
}
