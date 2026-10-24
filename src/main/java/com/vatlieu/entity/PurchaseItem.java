package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Getter @Setter
public class PurchaseItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "order_id")
    private PurchaseOrder order;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;
    /** Số lượng theo đơn vị gốc. */
    private BigDecimal qty;
    /** Đơn giá theo đơn vị gốc (chưa gồm cước vận chuyển). */
    private BigDecimal unitCost;
    private String entryMode = "WEIGHT";
    private String bagWeights;
    private BigDecimal lineAmount;
}
