package com.vatlieu.form;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboundForm {

  private String kind = "SALE";
  private String customer;
  private String note;
  private boolean settleAll;
  private BigDecimal paidAmount = BigDecimal.ZERO;
  private UUID requestKey = UUID.randomUUID();
  private List<PurchaseItemForm> items = new ArrayList<>();
}
