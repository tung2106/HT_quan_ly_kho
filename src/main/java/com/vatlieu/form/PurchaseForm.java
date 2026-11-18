package com.vatlieu.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PurchaseForm {

  private Long supplierId;
  private String sellerName;
  private boolean directEntry;
  private String productName;
  private String productGroup = "MATERIAL";
  private String baseUnit = "kg";
  private String lotCode;
  private java.util.UUID requestKey = java.util.UUID.randomUUID();

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate orderDate = LocalDate.now();

  private BigDecimal shippingCost;
  private boolean settleAll;
  private BigDecimal paidAmount;
  private String note;
  private List<PurchaseItemForm> items = new ArrayList<>();
}
