package com.vatlieu.service;

import com.vatlieu.form.PurchaseItemForm;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class AgriculturePurchaseTest {
    private PurchaseItemForm row(String mode) {
        var row = new PurchaseItemForm(); row.setEntryMode(mode); row.setUnit("kg");
        row.setUnitPrice(new BigDecimal("1000")); return row;
    }
    @Test void congCanTungBaoKhongQuyDoiCoDinh() {
        var r=row("BAGS"); r.setBagWeights("48,5\n52\n50.125");
        var result=AgriculturePurchase.calculate(r);
        assertThat(result.qty()).isEqualByComparingTo("150.625");
        assertThat(result.amount()).isEqualByComparingTo("150625");
        assertThat(result.bags()).isEqualTo("48.5\n52\n50.125");
    }
    @Test void caLoGiuTongTienVaGiaKgNhieuSoLe() {
        var r=row("LOT");r.setQty(new BigDecimal("3"));r.setLotAmount(new BigDecimal("100"));r.setUnitPrice(null);
        var result=AgriculturePurchase.calculate(r);
        assertThat(result.amount()).isEqualByComparingTo("100");
        assertThat(result.cost()).isEqualByComparingTo("33.333333333333");
    }
    @Test void chiLamTronSauCongTongPhieu() {
        var r=row("WEIGHT");r.setQty(new BigDecimal("0.004"));r.setUnitPrice(new BigDecimal("100"));
        var result=AgriculturePurchase.calculate(r);
        assertThat(result.amount()).isEqualByComparingTo("0.4");
        assertThat(InventoryMath.total(List.of(result.amount(),result.amount()))).isEqualByComparingTo("1");
    }
    @Test void tuChoiKgSaiThieuGiaVaQuyDoiBao() {
        for(String value : List.of("0","-1","0.0001","abc","1\n\n2")) {
            var r=row("BAGS");r.setBagWeights(value);
            assertThatThrownBy(() -> AgriculturePurchase.calculate(r)).isInstanceOf(IllegalArgumentException.class);
        }
        var r=row("LOT");r.setQty(BigDecimal.ONE);
        assertThatThrownBy(() -> AgriculturePurchase.calculate(r)).isInstanceOf(IllegalArgumentException.class);
        r.setLotAmount(BigDecimal.TEN);r.setUnit("bao");
        assertThatThrownBy(() -> AgriculturePurchase.calculate(r)).isInstanceOf(IllegalArgumentException.class);
    }
}
