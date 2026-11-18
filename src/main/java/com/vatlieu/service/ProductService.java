package com.vatlieu.service;

import com.vatlieu.entity.Product;
import com.vatlieu.entity.UnitConversion;
import com.vatlieu.form.ProductForm;
import com.vatlieu.repository.ProductRepository;
import com.vatlieu.repository.UnitConversionRepository;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
public class ProductService {

  private final ProductRepository products;
  private final JdbcTemplate jdbc;
  private final UnitConversionRepository conversions;
  private final jakarta.persistence.EntityManager entityManager;

  /** Dùng đúng hàng đã có; hàng mới được tạo trong cùng giao dịch với phiếu nhập. */
  @Transactional
  public Product forDirectPurchase(String name, String group, String unit) {
    if (name == null || name.isBlank() || name.strip().length() > 200) {
      throw new IllegalArgumentException("Tên hàng cần từ 1 đến 200 ký tự");
    }
    if (group == null || !Set.of("MATERIAL", "AGRICULTURE").contains(group)) {
      throw new IllegalArgumentException("Hãy chọn nhóm hàng");
    }
    String cleanName = java.text.Normalizer.normalize(name.strip(), java.text.Normalizer.Form.NFC);
    String base = "AGRICULTURE".equals(group) ? "kg" : unit;
    var allProducts = products.findAllByOrderByNameAsc();
    var sameNames = allProducts
      .stream()
      .filter(p -> p.getProductGroup().equals(group))
      .filter(p ->
        java.text.Normalizer.normalize(
          p.getName().strip(),
          java.text.Normalizer.Form.NFC
        ).equalsIgnoreCase(cleanName)
      )
      .toList();
    var matches = sameNames
      .stream()
      .filter(p -> p.getBaseUnit().equals(base))
      .toList();
    if (matches.size() > 1) throw new IllegalArgumentException(
      "Có nhiều mặt hàng trùng tên và đơn vị. Hãy đổi tên trong Sản phẩm để phân biệt trước khi nhập."
    );
    if (matches.size() == 1) {
      Product product = matches.get(0);
      entityManager.refresh(product, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
      if (!product.isActive()) throw new IllegalArgumentException(
        "Mặt hàng này đã ngừng sử dụng. Hãy kiểm tra lại tên hàng."
      );
      return product;
    }
    if (!sameNames.isEmpty()) throw new IllegalArgumentException(
      "Mặt hàng đã có nhưng khác đơn vị. Hãy chọn đúng đơn vị của hàng đã lưu."
    );
    if (
      base == null ||
      (!Set.of("kg", "viên", "bao", "m3", "m2", "m", "cái", "tấm", "cây").contains(base) &&
        allProducts.stream().noneMatch(p -> p.getBaseUnit().equals(base)))
    ) {
      throw new IllegalArgumentException("Hãy chọn đơn vị của mặt hàng mới");
    }
    ProductForm form = new ProductForm();
    form.setName(cleanName);
    form.setProductGroup(group);
    form.setBaseUnit(base);
    return save(form);
  }

  @Transactional
  public void addUnit(long productId, String unitName, BigDecimal factor) {
    Product product = products
      .findLockedById(productId)
      .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
    if (product.isAgriculture()) {
      throw new IllegalArgumentException(
        "Nông sản dùng kg cân thực tế, không dùng quy đổi bao cố định"
      );
    }
    if (factor == null || factor.signum() <= 0) {
      throw new IllegalArgumentException("Hệ số quy đổi phải > 0");
    }
    if (unitName == null || unitName.isBlank() || unitName.trim().length() > 20) {
      throw new IllegalArgumentException("Tên đơn vị cần từ 1 đến 20 ký tự");
    }
    String name = unitName.trim();
    if (conversions.findByProductIdAndUnitNameIgnoreCase(productId, name).isPresent()) {
      throw new IllegalArgumentException("Đơn vị này đã có quy đổi");
    }
    UnitConversion conversion = new UnitConversion();
    conversion.setProduct(product);
    conversion.setUnitName(name);
    conversion.setFactor(factor);
    conversions.save(conversion);
  }

  @Transactional
  public void deleteUnit(long productId, long conversionId) {
    products
      .findLockedById(productId)
      .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
    UnitConversion conversion = conversions
      .findById(conversionId)
      .filter(unit -> unit.getProduct().getId().equals(productId))
      .orElseThrow(() ->
        new IllegalArgumentException("Quy đổi không thuộc sản phẩm này hoặc đã bị xóa")
      );
    conversions.delete(conversion);
  }

  @Transactional
  public void delete(long id) {
    Product product = products
      .findLockedById(id)
      .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
    product.setActive(false);
    products.save(product);
  }

  @Transactional
  public Product save(ProductForm f) {
    if (
      !Set.of("MATERIAL", "AGRICULTURE").contains(f.getProductGroup())
    ) throw new IllegalArgumentException("Hãy chọn nhóm hàng");
    if (
      f.getName() == null || f.getName().isBlank() || f.getName().trim().length() > 200
    ) throw new IllegalArgumentException("Tên sản phẩm cần từ 1 đến 200 ký tự");
    String unit = "AGRICULTURE".equals(f.getProductGroup()) ? "kg" : f.getBaseUnit();
    if (
      unit == null || unit.isBlank() || unit.trim().length() > 20
    ) throw new IllegalArgumentException("Hãy chọn đơn vị gốc");
    unit = unit.trim();
    Product p =
      f.getId() == null
        ? new Product()
        : products
            .findLockedById(f.getId())
            .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
    if (
      p.getId() != null &&
      !unit.equals(p.getBaseUnit()) &&
      (jdbc.queryForObject(
        "SELECT COUNT(*) FROM stock_transaction WHERE product_id=?",
        Long.class,
        p.getId()
      ) > 0 ||
        jdbc.queryForObject(
          "SELECT COUNT(*) FROM unit_conversion WHERE product_id=?",
          Long.class,
          p.getId()
        ) > 0)
    ) throw new IllegalArgumentException(
      "Sản phẩm đã có giao dịch hoặc quy đổi; không thể đổi đơn vị gốc. Hãy tạo sản phẩm mới."
    );
    p.setProductGroup(f.getProductGroup());
    p.setName(f.getName().trim());
    p.setCode(f.getCode() == null || f.getCode().isBlank() ? null : f.getCode().trim());
    if (p.getCode() != null && p.getCode().length() > 50) throw new IllegalArgumentException(
      "Mã sản phẩm tối đa 50 ký tự"
    );
    p.setBaseUnit(unit);
    p.setRetailPrice(
      InventoryMath.price(f.getRetailPrice() == null ? BigDecimal.ZERO : f.getRetailPrice())
    );
    BigDecimal min = f.getMinStock() == null ? BigDecimal.ZERO : f.getMinStock();
    if (min.signum() < 0) throw new IllegalArgumentException("Tồn tối thiểu không được âm");
    p.setMinStock(
      min.signum() == 0 ? BigDecimal.ZERO : InventoryMath.quantity(min, BigDecimal.ONE)
    );
    p.setActive(f.isActive());
    p.setNote(f.getNote());
    return products.save(p);
  }
}
