package com.vatlieu.form;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class PurchaseItemForm {
    private Long productId;
    private BigDecimal qty;
    /** Để trống = đơn vị gốc. Nếu nhập (vd "bao") thì phải có quy đổi trong sản phẩm. */
    private String unit;
    /** Đơn giá theo đúng đơn vị đã chọn ở trên. */
    private BigDecimal unitPrice;
}
