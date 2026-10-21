package com.vatlieu.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class InventoryMathTest {
    private BigDecimal n(String value) { return new BigDecimal(value); }
    @Test void congTatCaRoiMoiLamTron() {
        assertThat(InventoryMath.total(List.of(n("0.4"),n("0.4")))).isEqualByComparingTo("1");
        assertThat(InventoryMath.total(List.of(n("10.5")))).isEqualByComparingTo("11");
        assertThat(InventoryMath.total(List.of(n("10.49")))).isEqualByComparingTo("10");
    }
    @Test void quyDoiBaoVaSoLe() {
        assertThat(InventoryMath.quantity(n("1.25"),n("50"))).isEqualByComparingTo("62.5");
        assertThat(InventoryMath.quantity(n("0.001"),BigDecimal.ONE)).isEqualByComparingTo("0.001");
        assertThatThrownBy(() -> InventoryMath.quantity(n("0.0001"),BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InventoryMath.quantity(n("0"),BigDecimal.ONE)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void fifoNhieuLoGiaKhacNhauVaBanDungTon() {
        var lots = List.of(new InventoryMath.Lot(1,n("1.5"),n("100")),new InventoryMath.Lot(2,n("2"),n("200")));
        var takes = InventoryMath.fifo(n("2"),lots);
        assertThat(takes).hasSize(2);
        assertThat(takes.get(0).qty()).isEqualByComparingTo("1.5");
        assertThat(InventoryMath.cost(takes)).isEqualByComparingTo("250");
        assertThat(InventoryMath.cost(InventoryMath.fifo(n("3.5"),lots))).isEqualByComparingTo("550");
        assertThatThrownBy(() -> InventoryMath.fifo(n("3.501"),lots)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void thuTraTungPhanVaVuotNo() {
        assertThat(InventoryMath.payment(n("40"),n("100"))).isEqualByComparingTo("40");
        assertThat(InventoryMath.payment(n("100"),n("100"))).isEqualByComparingTo("100");
        for (String value : List.of("101","-1","0.5")) {
            assertThatThrownBy(() -> InventoryMath.payment(n(value),n("100"))).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void tuChoiGiaAmVaGiaQuaChinhXac() {
        assertThatThrownBy(() -> InventoryMath.price(n("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InventoryMath.price(n("0.001"))).isInstanceOf(IllegalArgumentException.class);
    }
}
