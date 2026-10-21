package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Getter @Setter
public class Batch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne @JoinColumn(name = "purchase_item_id")
    private PurchaseItem purchaseItem;
    private LocalDate receivedDate;
    private BigDecimal qtyIn;
    private BigDecimal qtyLeft;
    /** Giá vốn/đơn vị gốc đã gồm phân bổ cước vận chuyển. */
    private BigDecimal unitCost;
}
