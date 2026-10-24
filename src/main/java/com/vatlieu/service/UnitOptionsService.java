package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class UnitOptionsService {
    private final JdbcTemplate jdbc;
    public record Choice(Long productId, String unit, String label) {}

    /** Chỉ đưa các đơn vị đã khai báo ra biểu mẫu, ưu tiên đơn vị gốc. */
    public List<Choice> findAll() {
        return jdbc.query("""
                SELECT p.id, p.base_unit AS unit, p.base_unit AS label, true AS is_base
                FROM product p WHERE p.active
                UNION ALL
                SELECT p.id, u.unit_name, u.unit_name || ' = ' || trim_scale(u.factor)::text || ' ' || p.base_unit, false
                FROM unit_conversion u JOIN product p ON p.id=u.product_id
                WHERE p.active AND p.product_group='MATERIAL' AND lower(u.unit_name)<>lower(p.base_unit)
                ORDER BY id, is_base DESC, unit
                """, (rs,n) -> new Choice(rs.getLong("id"), rs.getString("unit"), rs.getString("label")));
    }
}
