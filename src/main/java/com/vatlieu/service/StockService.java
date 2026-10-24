package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/** Đọc tồn kho từ view v_stock (tính từ lịch sử giao dịch). */
@Service
@RequiredArgsConstructor
public class StockService {
    private final JdbcTemplate jdbc;

    public record StockRow(Long productId, String name, String baseUnit,
                           BigDecimal qtyOnHand, BigDecimal minStock,
                           boolean lowStock, BigDecimal avgCost, BigDecimal lotValue, boolean active) {
        public BigDecimal value() {
            return lotValue;
        }
    }

    public List<java.util.Map<String,Object>> lots(boolean includeCost) {
        var rows = jdbc.queryForList("""
                SELECT b.id,b.product_id,p.name,p.base_unit,p.active,b.received_date,b.qty_in,b.qty_left,
                       b.unit_cost,pi.order_id AS purchase_id,COALESCE(s.name,'Không rõ NCC') AS supplier
                FROM batch b JOIN product p ON p.id=b.product_id
                LEFT JOIN purchase_item pi ON pi.id=b.purchase_item_id
                LEFT JOIN purchase_order o ON o.id=pi.order_id
                LEFT JOIN supplier s ON s.id=o.supplier_id
                WHERE b.qty_left>0 ORDER BY p.name,b.received_date,b.id
                """);
        if (!includeCost) rows.forEach(row -> row.remove("unit_cost"));
        return rows;
    }

    public List<StockRow> findAll() {
        return jdbc.query("""
                SELECT s.product_id, s.name, s.base_unit, s.qty_on_hand, s.min_stock,
                       s.low_stock, p.avg_cost, p.active,
                       COALESCE((SELECT SUM(b.qty_left*b.unit_cost) FROM batch b WHERE b.product_id=p.id),0) AS lot_value
                FROM v_stock s JOIN product p ON p.id = s.product_id
                WHERE p.active OR s.qty_on_hand <> 0
                ORDER BY s.low_stock DESC, s.name
                """,
                (rs, i) -> new StockRow(
                        rs.getLong("product_id"), rs.getString("name"), rs.getString("base_unit"),
                        rs.getBigDecimal("qty_on_hand"), rs.getBigDecimal("min_stock"),
                        rs.getBoolean("low_stock"), rs.getBigDecimal("avg_cost"), rs.getBigDecimal("lot_value"), rs.getBoolean("active")));
    }
}
