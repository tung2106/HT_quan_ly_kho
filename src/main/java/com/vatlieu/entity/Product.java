package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Getter @Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "category_id")
    private Category category;
    private String productGroup = "MATERIAL";
    public boolean isAgriculture() { return "AGRICULTURE".equals(productGroup); }
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String baseUnit;
    private BigDecimal minStock = BigDecimal.ZERO;
    private BigDecimal avgCost = BigDecimal.ZERO;
    private BigDecimal retailPrice = BigDecimal.ZERO;
    private boolean active = true;
    private String note;
}
