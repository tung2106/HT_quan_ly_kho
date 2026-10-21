package com.vatlieu.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Quy tắc số lượng, tiền và phân bổ FIFO dùng chung cho xuất bán/hao hụt. */
public final class InventoryMath {
    private InventoryMath() {}
    public record Lot(long id, BigDecimal qty, BigDecimal cost) {}
    public record Take(long id, BigDecimal qty, BigDecimal cost) {}

    public static BigDecimal quantity(BigDecimal qty, BigDecimal factor) {
        if (qty == null || qty.signum() <= 0 || factor == null || factor.signum() <= 0)
            throw new IllegalArgumentException("Số lượng và hệ số quy đổi phải lớn hơn 0");
        BigDecimal result = qty.multiply(factor);
        try { result = result.setScale(3, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Số lượng theo đơn vị gốc chỉ được tối đa 3 số lẻ"); }
        if (result.precision() > 15) throw new IllegalArgumentException("Số lượng quá lớn");
        return result;
    }

    public static BigDecimal price(BigDecimal value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("Đơn giá phải từ 0 trở lên");
        try { value = value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Đơn giá chỉ được tối đa 2 số lẻ"); }
        if (value.precision() > 15) throw new IllegalArgumentException("Đơn giá quá lớn");
        return value;
    }

    public static BigDecimal total(List<BigDecimal> lines) {
        return lines.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal payment(BigDecimal value, BigDecimal remaining) {
        if (value == null || value.signum() < 0 || value.compareTo(remaining) > 0)
            throw new IllegalArgumentException("Tiền thanh toán phải từ 0 đến số tiền còn nợ");
        try { return value.setScale(0, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Tiền thanh toán phải là số nguyên đồng"); }
    }

    public static List<Take> fifo(BigDecimal quantity, List<Lot> lots) {
        if (quantity == null || quantity.signum() <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        List<Take> result = new ArrayList<>();
        BigDecimal left = quantity;
        for (Lot lot : lots) {
            if (left.signum() == 0) break;
            BigDecimal take = left.min(lot.qty());
            if (take.signum() > 0) result.add(new Take(lot.id(), take, lot.cost()));
            left = left.subtract(take);
        }
        if (left.signum() > 0) throw new IllegalArgumentException("Không đủ tồn kho để xuất");
        return result;
    }

    public static BigDecimal cost(List<Take> takes) {
        return takes.stream().map(t -> t.qty().multiply(t.cost())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
