package com.vatlieu.form;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class PurchaseItemForm {
    private Long productId;
    /** Lô được chọn khi xuất bán; để trống để xuất FIFO. */
    private Long batchId;
    private BigDecimal qty;
    /** Để trống = đơn vị gốc. Nếu nhập (vd "bao") thì phải có quy đổi trong sản phẩm. */
    private String unit;
    /** Đơn giá theo đúng đơn vị đã chọn ở trên. */
    private BigDecimal unitPrice;
    private String entryMode = "WEIGHT";
    /** Mỗi dòng là số kg của một bao, không phải hệ số quy đổi. */
    private String bagWeights;
    private BigDecimal lotAmount;
}
