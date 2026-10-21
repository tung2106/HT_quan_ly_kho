package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 1 [unitName] = [factor] đơn vị gốc của sản phẩm. */
@Entity @Getter @Setter
public class UnitConversion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "product_id")
    private Product product;
    @Column(nullable = false)
    private String unitName;
    @Column(nullable = false)
    private BigDecimal factor;
}
