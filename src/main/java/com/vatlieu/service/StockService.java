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
                           boolean lowStock, BigDecimal avgCost) {
        public BigDecimal value() {
            return qtyOnHand.multiply(avgCost);
        }
    }

    public List<StockRow> findAll() {
        return jdbc.query("""
                SELECT s.product_id, s.name, s.base_unit, s.qty_on_hand, s.min_stock,
                       s.low_stock, p.avg_cost
                FROM v_stock s JOIN product p ON p.id = s.product_id
                WHERE p.active
                ORDER BY s.low_stock DESC, s.name
                """,
                (rs, i) -> new StockRow(
                        rs.getLong("product_id"), rs.getString("name"), rs.getString("base_unit"),
                        rs.getBigDecimal("qty_on_hand"), rs.getBigDecimal("min_stock"),
                        rs.getBoolean("low_stock"), rs.getBigDecimal("avg_cost")));
    }
}
