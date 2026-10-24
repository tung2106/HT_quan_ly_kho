package com.vatlieu.form;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class PurchaseForm {
    private Long supplierId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDate = LocalDate.now();
    private BigDecimal shippingCost;
    private boolean settleAll;
    private BigDecimal paidAmount;
    private String note;
    private List<PurchaseItemForm> items = new ArrayList<>();
}
