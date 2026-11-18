package com.vatlieu.form;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductForm {

  private Long id;
  private Long categoryId;
  private String productGroup = "MATERIAL";
  private String code;
  private String name;
  private String baseUnit;
  private BigDecimal minStock = BigDecimal.ZERO;
  private BigDecimal retailPrice = BigDecimal.ZERO;
  private boolean active = true;
  private String note;
}
