package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Getter @Setter
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    private LocalDate orderDate = LocalDate.now();
    private BigDecimal shippingCost = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private String note;
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

    /** Tổng tiền hàng + cước vận chuyển. */
    public BigDecimal getTotal() {
        if (totalAmount != null) return totalAmount;
        return items.stream()
                .map(i -> i.getQty().multiply(i.getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(shippingCost);
    }
}
