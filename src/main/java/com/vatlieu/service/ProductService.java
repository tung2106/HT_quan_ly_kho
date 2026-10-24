package com.vatlieu.service;

import com.vatlieu.entity.Product;
import com.vatlieu.form.ProductForm;
import com.vatlieu.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Set;

@Service @RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
public class ProductService {
    private final ProductRepository products;
    private final JdbcTemplate jdbc;

    @Transactional
    public void delete(long id) {
        Product product = products.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
        product.setActive(false);
        products.save(product);
    }

    @Transactional
    public Product save(ProductForm f) {
        if (!Set.of("MATERIAL", "AGRICULTURE").contains(f.getProductGroup()))
            throw new IllegalArgumentException("Hãy chọn nhóm hàng");
        if (f.getName() == null || f.getName().isBlank() || f.getName().trim().length() > 200)
            throw new IllegalArgumentException("Tên sản phẩm cần từ 1 đến 200 ký tự");
        String unit = "AGRICULTURE".equals(f.getProductGroup()) ? "kg" : f.getBaseUnit();
        if (unit == null || unit.isBlank() || unit.trim().length() > 20)
            throw new IllegalArgumentException("Hãy chọn đơn vị gốc");
        unit = unit.trim();
        Product p = f.getId() == null ? new Product() : products.findLockedById(f.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
        if (p.getId() != null && !unit.equals(p.getBaseUnit()) &&
                (jdbc.queryForObject("SELECT COUNT(*) FROM stock_transaction WHERE product_id=?", Long.class, p.getId()) > 0
                || jdbc.queryForObject("SELECT COUNT(*) FROM unit_conversion WHERE product_id=?", Long.class, p.getId()) > 0))
            throw new IllegalArgumentException("Sản phẩm đã có giao dịch hoặc quy đổi; không thể đổi đơn vị gốc. Hãy tạo sản phẩm mới.");
        p.setProductGroup(f.getProductGroup());
        p.setName(f.getName().trim());
        p.setCode(f.getCode() == null || f.getCode().isBlank() ? null : f.getCode().trim());
        if (p.getCode() != null && p.getCode().length() > 50) throw new IllegalArgumentException("Mã sản phẩm tối đa 50 ký tự");
        p.setBaseUnit(unit);
        p.setRetailPrice(InventoryMath.price(f.getRetailPrice() == null ? BigDecimal.ZERO : f.getRetailPrice()));
        BigDecimal min = f.getMinStock() == null ? BigDecimal.ZERO : f.getMinStock();
        if (min.signum() < 0) throw new IllegalArgumentException("Tồn tối thiểu không được âm");
        p.setMinStock(min.signum() == 0 ? BigDecimal.ZERO : InventoryMath.quantity(min, BigDecimal.ONE));
        p.setActive(f.isActive());
        p.setNote(f.getNote());
        return products.save(p);
    }
}
