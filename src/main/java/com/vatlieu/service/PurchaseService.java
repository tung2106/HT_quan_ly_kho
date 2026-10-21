package com.vatlieu.service;

import com.vatlieu.entity.*;
import com.vatlieu.form.PurchaseForm;
import com.vatlieu.form.PurchaseItemForm;
import com.vatlieu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

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

    private record Line(Product product, BigDecimal baseQty, BigDecimal baseCost, BigDecimal value) {}

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER')")
    public PurchaseOrder create(PurchaseForm f) {
        List<PurchaseItemForm> rows = f.getItems().stream()
                .filter(i -> i.getProductId() != null || i.getQty() != null || i.getUnitPrice() != null)
                .toList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một dòng hàng có số lượng > 0");
        }

        List<Line> lines = new ArrayList<>();
        Map<Long, Product> locked = new HashMap<>();
        for (Long id : rows.stream().map(PurchaseItemForm::getProductId).filter(java.util.Objects::nonNull).distinct().sorted().toList()) {
            locked.put(id, productRepo.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại")));
        }
        for (PurchaseItemForm r : rows) {
            Product p = locked.get(r.getProductId());
            if (p == null || !p.isActive()) throw new IllegalArgumentException("Hãy chọn sản phẩm đang sử dụng");
            if (r.getUnitPrice() == null || r.getUnitPrice().signum() < 0) {
                throw new IllegalArgumentException("Thiếu đơn giá cho: " + p.getName());
            }
            BigDecimal factor = factorOf(p, r.getUnit());
            BigDecimal baseQty = InventoryMath.quantity(r.getQty(), factor);
            InventoryMath.price(r.getUnitPrice());
            BigDecimal baseCost = r.getUnitPrice().divide(factor, 4, RoundingMode.HALF_UP);
            lines.add(new Line(p, baseQty, baseCost, r.getQty().multiply(r.getUnitPrice())));
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderDate(f.getOrderDate() != null ? f.getOrderDate() : LocalDate.now());
        order.setShippingCost(nz(f.getShippingCost()));
        order.setPaidAmount(nz(f.getPaidAmount()));
        order.setNote(f.getNote());
        if (f.getSupplierId() != null) {
            if (jdbc.queryForList("SELECT id FROM supplier WHERE id=? AND active=true FOR SHARE", f.getSupplierId()).isEmpty())
                throw new IllegalArgumentException("Nhà cung cấp đã bị xóa khỏi danh sách hoặc không tồn tại. Hãy chọn lại.");
            order.setSupplier(supplierRepo.getReferenceById(f.getSupplierId()));
        }

        BigDecimal shipping = order.getShippingCost();
        BigDecimal totalValue = lines.stream().map(Line::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        InventoryMath.price(shipping);
        order.setTotalAmount(InventoryMath.total(List.of(totalValue, shipping)));
        InventoryMath.payment(order.getPaidAmount(), order.getTotalAmount());
        List<BigDecimal> landedCosts = new ArrayList<>();

        for (Line l : lines) {
            BigDecimal share = totalValue.signum() > 0
                    ? shipping.multiply(l.value()).divide(totalValue, 4, RoundingMode.HALF_UP)
                    : shipping.divide(BigDecimal.valueOf(lines.size()), 4, RoundingMode.HALF_UP);
            BigDecimal landed = l.baseCost()
                    .add(share.divide(l.baseQty(), 4, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
            landedCosts.add(landed);

            PurchaseItem item = new PurchaseItem();
            item.setOrder(order);
            item.setProduct(l.product());
            item.setQty(l.baseQty());
            item.setUnitCost(l.baseCost().setScale(2, RoundingMode.HALF_UP));
            order.getItems().add(item);
        }
        orderRepo.save(order);
        if (order.getPaidAmount().signum() > 0) {
            jdbc.update("INSERT INTO cash_payment(purchase_id,amount,note,request_key) VALUES (?,?,?,?)",
                    order.getId(),order.getPaidAmount(),"Trả lúc nhập hàng",UUID.randomUUID());
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
        return convRepo.findByProductIdAndUnitNameIgnoreCase(p.getId(), unit.trim())
                .map(UnitConversion::getFactor)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chưa khai báo quy đổi đơn vị '" + unit.trim() + "' cho: " + p.getName()));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
