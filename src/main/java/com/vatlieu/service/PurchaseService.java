package com.vatlieu.service;

import com.vatlieu.entity.*;
import com.vatlieu.form.PurchaseForm;
import com.vatlieu.form.PurchaseItemForm;
import com.vatlieu.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhập hàng:
 *  1. Quy đổi số lượng/đơn giá về ĐƠN VỊ GỐC.
 *  2. Phân bổ cước vận chuyển vào từng dòng theo tỉ lệ giá trị -> giá vốn lô (landed cost).
 *  3. Tạo lô (Batch) + giao dịch kho (PURCHASE).
 *  4. Cập nhật giá vốn bình quân gia quyền của sản phẩm.
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

  private final ProductRepository productRepo;
  private final UnitConversionRepository convRepo;
  private final SupplierRepository supplierRepo;
  private final PurchaseOrderRepository orderRepo;
  private final BatchRepository batchRepo;
  private final StockTransactionRepository txRepo;
  private final JdbcTemplate jdbc;
  private final ProductService products;

  @Transactional(readOnly = true)
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
  public List<PurchaseOrder> history(String keyword) {
    String query = SearchText.normalize(keyword);
    return orderRepo
      .findAllByOrderByOrderDateDescIdDesc()
      .stream()
      .filter(
        order ->
          query.isEmpty() ||
          SearchText.normalize(order.getSellerDisplayName()).contains(query) ||
          SearchText.normalize(order.getLotCode()).contains(query) ||
          order
            .getItems()
            .stream()
            .anyMatch(item -> SearchText.normalize(item.getProduct().getName()).contains(query))
      )
      .toList();
  }

  public String nextLotCode() {
    String code;
    do {
      code =
        "NH-" +
        String.format(
          java.util.Locale.ROOT,
          "%06d",
          jdbc.queryForObject("SELECT nextval('purchase_lot_code_seq')", Long.class)
        );
    } while (
      Boolean.TRUE.equals(
        jdbc.queryForObject(
          "SELECT EXISTS(SELECT 1 FROM purchase_order WHERE lot_code=?)",
          Boolean.class,
          code
        )
      )
    );
    return code;
  }

  private record Line(
    Product product,
    BigDecimal baseQty,
    BigDecimal baseCost,
    BigDecimal value,
    String mode,
    String bags
  ) {}

  @Transactional
  @org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
  public PurchaseOrder create(PurchaseForm f) {
    String sellerName = f.getSellerName() == null ? "" : f.getSellerName().strip();
    if (sellerName.length() > 150) throw new IllegalArgumentException(
      "Tên người bán tối đa 150 ký tự"
    );
    if (f.getRequestKey() == null) throw new IllegalArgumentException(
      "Thiếu mã lần lưu, hãy mở lại phiếu nhập"
    );
    // Cửa hàng nhỏ: tuần tự hóa nhập để tránh trùng tên hàng/mã lô khi hai người cùng lưu.
    jdbc.queryForList("SELECT pg_advisory_xact_lock(834702)");
    var saved = jdbc.queryForList(
      "SELECT id FROM purchase_order WHERE request_key=?",
      Long.class,
      f.getRequestKey()
    );
    if (!saved.isEmpty()) return orderRepo.findById(saved.get(0)).orElseThrow();
    String lotCode =
      f.getLotCode() == null || f.getLotCode().isBlank()
        ? nextLotCode()
        : f.getLotCode().strip().toUpperCase(java.util.Locale.ROOT);
    if (lotCode.length() > 50) throw new IllegalArgumentException("Mã lô tối đa 50 ký tự");
    if (
      Boolean.TRUE.equals(
        jdbc.queryForObject(
          "SELECT EXISTS(SELECT 1 FROM purchase_order WHERE lot_code=?)",
          Boolean.class,
          lotCode
        )
      )
    ) {
      throw new IllegalArgumentException("Mã lô đã được dùng. Hãy nhập mã khác.");
    }
    if (f.isDirectEntry()) {
      if (f.getItems() == null || f.getItems().size() != 1) throw new IllegalArgumentException(
        "Mỗi lần nhập chỉ ghi một mặt hàng"
      );
      Product product = products.forDirectPurchase(
        f.getProductName(),
        f.getProductGroup(),
        f.getBaseUnit()
      );
      f.getItems().get(0).setProductId(product.getId());
      f.getItems().get(0).setUnit(product.getBaseUnit());
    }
    if (f.getItems() == null || f.getItems().size() > 100) throw new IllegalArgumentException(
      "Mỗi phiếu tối đa 100 dòng hàng"
    );
    List<PurchaseItemForm> rows = f
      .getItems()
      .stream()
      .filter(
        i ->
          i.getProductId() != null ||
          i.getQty() != null ||
          i.getUnitPrice() != null ||
          i.getLotAmount() != null ||
          (i.getBagWeights() != null && !i.getBagWeights().isBlank())
      )
      .toList();
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("Cần ít nhất một dòng hàng có số lượng > 0");
    }

    List<Line> lines = new ArrayList<>();
    Map<Long, Product> locked = new HashMap<>();
    for (Long id : rows
      .stream()
      .map(PurchaseItemForm::getProductId)
      .filter(java.util.Objects::nonNull)
      .distinct()
      .sorted()
      .toList()) {
      locked.put(
        id,
        productRepo
          .findLockedById(id)
          .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"))
      );
    }
    for (PurchaseItemForm r : rows) {
      Product p = locked.get(r.getProductId());
      if (p == null || !p.isActive()) throw new IllegalArgumentException(
        "Hãy chọn sản phẩm đang sử dụng"
      );
      if (p.isAgriculture()) {
        if (!"kg".equals(p.getBaseUnit())) throw new IllegalArgumentException(
          "Nông sản cần đơn vị gốc kg"
        );
        var result = AgriculturePurchase.calculate(r);
        lines.add(
          new Line(p, result.qty(), result.cost(), result.amount(), r.getEntryMode(), result.bags())
        );
        continue;
      }
      if (!"WEIGHT".equals(r.getEntryMode())) throw new IllegalArgumentException(
        "Vật liệu xây dựng dùng số lượng và đơn giá theo đơn vị đã chọn"
      );
      if (r.getUnitPrice() == null || r.getUnitPrice().signum() < 0) {
        throw new IllegalArgumentException("Thiếu đơn giá cho: " + p.getName());
      }
      BigDecimal factor = factorOf(p, r.getUnit());
      BigDecimal baseQty = InventoryMath.quantity(r.getQty(), factor);
      InventoryMath.price(r.getUnitPrice());
      BigDecimal baseCost = r.getUnitPrice().divide(factor, 4, RoundingMode.HALF_UP);
      lines.add(
        new Line(p, baseQty, baseCost, r.getQty().multiply(r.getUnitPrice()), "WEIGHT", null)
      );
    }

    PurchaseOrder order = new PurchaseOrder();
    order.setOrderDate(f.getOrderDate() != null ? f.getOrderDate() : LocalDate.now());
    order.setShippingCost(nz(f.getShippingCost()));
    order.setPaidAmount(nz(f.getPaidAmount()));
    order.setNote(f.getNote());
    order.setSellerName(sellerName.isEmpty() ? null : sellerName);
    order.setLotCode(lotCode);
    order.setRequestKey(f.getRequestKey());
    if (f.getSupplierId() != null) {
      if (
        jdbc
          .queryForList(
            "SELECT id FROM supplier WHERE id=? AND active=true FOR SHARE",
            f.getSupplierId()
          )
          .isEmpty()
      ) throw new IllegalArgumentException(
        "Nhà cung cấp đã bị xóa khỏi danh sách hoặc không tồn tại. Hãy chọn lại."
      );
      order.setSupplier(supplierRepo.getReferenceById(f.getSupplierId()));
    }

    BigDecimal shipping = order.getShippingCost();
    BigDecimal totalValue = lines
      .stream()
      .map(Line::value)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    InventoryMath.price(shipping);
    order.setTotalAmount(InventoryMath.total(List.of(totalValue, shipping)));
    if (f.isSettleAll()) order.setPaidAmount(order.getTotalAmount());
    InventoryMath.payment(order.getPaidAmount(), order.getTotalAmount());
    List<BigDecimal> landedCosts = new ArrayList<>();

    for (Line l : lines) {
      BigDecimal share =
        totalValue.signum() > 0
          ? shipping.multiply(l.value()).divide(totalValue, 12, RoundingMode.HALF_UP)
          : shipping.divide(BigDecimal.valueOf(lines.size()), 12, RoundingMode.HALF_UP);
      BigDecimal landed = l
        .baseCost()
        .add(share.divide(l.baseQty(), 12, RoundingMode.HALF_UP))
        .setScale(l.product().isAgriculture() ? 12 : 2, RoundingMode.HALF_UP);
      landedCosts.add(landed);

      PurchaseItem item = new PurchaseItem();
      item.setOrder(order);
      item.setProduct(l.product());
      item.setQty(l.baseQty());
      item.setUnitCost(
        l.baseCost().setScale(l.product().isAgriculture() ? 12 : 2, RoundingMode.HALF_UP)
      );
      item.setEntryMode(l.mode());
      item.setBagWeights(l.bags());
      item.setLineAmount(l.value());
      order.getItems().add(item);
    }
    orderRepo.save(order);
    if (order.getPaidAmount().signum() > 0) {
      jdbc.update(
        "INSERT INTO cash_payment(purchase_id,amount,note,request_key) VALUES (?,?,?,?)",
        order.getId(),
        order.getPaidAmount(),
        "Trả lúc nhập hàng",
        UUID.randomUUID()
      );
    }

    for (int i = 0; i < lines.size(); i++) {
      Line l = lines.get(i);
      Product p = l.product();
      BigDecimal landed = landedCosts.get(i);

      // Tồn trước khi nhập dòng này (để tính bình quân gia quyền)
      BigDecimal onHand = txRepo.sumQty(p.getId());
      BigDecimal base = (onHand == null ? BigDecimal.ZERO : onHand).max(BigDecimal.ZERO);
      BigDecimal oldValue = nz(batchRepo.remainingValue(p.getId()));

      Batch b = new Batch();
      b.setProduct(p);
      b.setPurchaseItem(order.getItems().get(i));
      b.setReceivedDate(order.getOrderDate());
      b.setQtyIn(l.baseQty());
      b.setQtyLeft(l.baseQty());
      b.setUnitCost(landed);
      batchRepo.save(b);

      StockTransaction tx = new StockTransaction();
      tx.setProduct(p);
      tx.setBatch(b);
      tx.setTxType(StockTransaction.PURCHASE);
      tx.setQtyChange(l.baseQty());
      tx.setUnitCost(landed);
      tx.setRefId(order.getId());
      tx.setNote("Nhập hàng #" + order.getId());
      txRepo.save(tx);

      BigDecimal newAvg = oldValue
        .add(l.baseQty().multiply(landed))
        .divide(base.add(l.baseQty()), 2, RoundingMode.HALF_UP);
      p.setAvgCost(newAvg);
      productRepo.save(p);
    }
    return order;
  }

  private BigDecimal factorOf(Product p, String unit) {
    if (unit == null || unit.isBlank() || unit.trim().equalsIgnoreCase(p.getBaseUnit())) {
      return BigDecimal.ONE;
    }
    return convRepo
      .findByProductIdAndUnitNameIgnoreCase(p.getId(), unit.trim())
      .map(UnitConversion::getFactor)
      .orElseThrow(() ->
        new IllegalArgumentException(
          "Chưa khai báo quy đổi đơn vị '" + unit.trim() + "' cho: " + p.getName()
        )
      );
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
