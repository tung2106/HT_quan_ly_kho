package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Getter @Setter
public class StockTransaction {
    public static final String PURCHASE = "PURCHASE";
    public static final String SALE = "SALE";
    public static final String SALE_RETURN = "SALE_RETURN";
    public static final String WASTE = "WASTE";
    public static final String ADJUSTMENT = "ADJUSTMENT";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne @JoinColumn(name = "batch_id")
    private Batch batch;
    @Column(nullable = false)
    private String txType;
    /** Dương = nhập, âm = xuất. */
    private BigDecimal qtyChange;
    private BigDecimal unitCost;
    private Long refId;
    private LocalDateTime txTime = LocalDateTime.now();
    private String note;
}
