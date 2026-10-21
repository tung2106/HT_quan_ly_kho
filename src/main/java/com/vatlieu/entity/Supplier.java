package com.vatlieu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String phone;
    private String note;
}
