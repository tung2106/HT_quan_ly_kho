package com.vatlieu.service;

import com.vatlieu.form.PurchaseItemForm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Chuẩn hóa nhập nông sản về kg; không làm tròn tiền từng dòng. */
public final class AgriculturePurchase {
    private AgriculturePurchase() {}
    public record Result(BigDecimal qty, BigDecimal amount, BigDecimal cost, String bags) {}

    public static Result calculate(PurchaseItemForm row) {
        String mode = row.getEntryMode();
        if (!List.of("WEIGHT", "BAGS", "LOT").contains(mode))
            throw new IllegalArgumentException("Cách nhập nông sản không hợp lệ");
        if (row.getUnit() != null && !row.getUnit().isBlank() && !"kg".equals(row.getUnit()))
            throw new IllegalArgumentException("Nông sản phải nhập kg cân thực tế, không quy đổi bao");
        BigDecimal qty;
        String bags = null;
        if ("BAGS".equals(mode)) {
            if (row.getBagWeights() == null || row.getBagWeights().isBlank() || row.getBagWeights().length() > 20000)
                throw new IllegalArgumentException("Hãy nhập kg từng bao, mỗi dòng một bao");
            String[] values = row.getBagWeights().trim().split("\\R");
            if (values.length > 1000) throw new IllegalArgumentException("Tối đa 1000 bao mỗi dòng hàng");
            List<String> normalized = new ArrayList<>();
            qty = BigDecimal.ZERO;
            for (String value : values) {
                try {
                    BigDecimal weight = InventoryMath.quantity(new BigDecimal(value.trim().replace(',', '.')), BigDecimal.ONE);
                    qty = qty.add(weight);
                    normalized.add(weight.stripTrailingZeros().toPlainString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Kg từng bao không hợp lệ; mỗi dòng chỉ nhập một số");
                }
            }
            qty = InventoryMath.quantity(qty, BigDecimal.ONE);
            bags = String.join("\n", normalized);
        } else qty = InventoryMath.quantity(row.getQty(), BigDecimal.ONE);
        BigDecimal amount = "LOT".equals(mode) ? InventoryMath.price(row.getLotAmount())
                : qty.multiply(InventoryMath.price(row.getUnitPrice()));
        return new Result(qty, amount, amount.divide(qty, 12, RoundingMode.HALF_UP), bags);
    }
}
