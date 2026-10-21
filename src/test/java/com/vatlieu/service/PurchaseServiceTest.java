package com.vatlieu.service;

import com.vatlieu.entity.*;
import com.vatlieu.form.PurchaseForm;
import com.vatlieu.form.PurchaseItemForm;
import com.vatlieu.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {
    @Mock ProductRepository productRepo;
    @Mock UnitConversionRepository convRepo;
    @Mock SupplierRepository supplierRepo;
    @Mock PurchaseOrderRepository orderRepo;
    @Mock BatchRepository batchRepo;
    @Mock StockTransactionRepository txRepo;
    @InjectMocks PurchaseService service;

    private Product product;
    private PurchaseForm form;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Xi măng PC40");
        product.setBaseUnit("kg");
        PurchaseItemForm item = new PurchaseItemForm();
        item.setProductId(1L);
        item.setQty(new BigDecimal("20"));
        item.setUnit("bao");
        item.setUnitPrice(new BigDecimal("90000"));
        form = new PurchaseForm();
        form.getItems().add(item);
    }

    private void mockProductAndConversion() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        UnitConversion conversion = new UnitConversion();
        conversion.setFactor(new BigDecimal("50"));
        when(convRepo.findByProductIdAndUnitNameIgnoreCase(1L, "bao"))
                .thenReturn(Optional.of(conversion));
    }

    @Test
    void nhapHaiMuoiBaoGhiMotNghinKgVaGiaVon1800() {
        mockProductAndConversion();
        // Kho chưa có giao dịch, cước và số tiền trả để trống; bỏ qua dòng form trống.
        form.getItems().add(new PurchaseItemForm());
        PurchaseOrder order = service.create(form);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getQty()).isEqualByComparingTo("1000");
        assertThat(order.getItems().get(0).getUnitCost()).isEqualByComparingTo("1800");
        assertThat(order.getTotal()).isEqualByComparingTo("1800000");
        assertThat(order.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(product.getAvgCost()).isEqualByComparingTo("1800");

        ArgumentCaptor<Batch> batch = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepo).save(batch.capture());
        assertThat(batch.getValue().getQtyIn()).isEqualByComparingTo("1000");
        assertThat(batch.getValue().getQtyLeft()).isEqualByComparingTo("1000");
        assertThat(batch.getValue().getUnitCost()).isEqualByComparingTo("1800");
        ArgumentCaptor<StockTransaction> tx = ArgumentCaptor.forClass(StockTransaction.class);
        verify(txRepo).save(tx.capture());
        assertThat(tx.getValue().getQtyChange()).isEqualByComparingTo("1000");
        assertThat(tx.getValue().getUnitCost()).isEqualByComparingTo("1800");
        assertThat(tx.getValue().getTxType()).isEqualTo(StockTransaction.PURCHASE);
        assertThat(tx.getValue().getBatch()).isSameAs(batch.getValue());
        assertThat(new StockService.StockRow(1L, product.getName(), "kg",
                new BigDecimal("1000"), BigDecimal.ZERO, false, product.getAvgCost()).value())
                .isEqualByComparingTo("1800000");
    }

    @Test
    void giaVonBinhQuanTinhCaTonCuVaCuocVanChuyen() {
        mockProductAndConversion();
        when(txRepo.sumQty(1L)).thenReturn(new BigDecimal("500"));
        product.setAvgCost(new BigDecimal("1500"));
        form.setShippingCost(new BigDecimal("100000"));

        PurchaseOrder order = service.create(form);

        assertThat(order.getTotal()).isEqualByComparingTo("1900000");
        assertThat(product.getAvgCost()).isEqualByComparingTo("1766.67");
        ArgumentCaptor<Batch> batch = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepo).save(batch.capture());
        assertThat(batch.getValue().getUnitCost()).isEqualByComparingTo("1900");
    }

    @Test
    void donViChuaKhaiBaoKhongGhiDonHayKho() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(convRepo.findByProductIdAndUnitNameIgnoreCase(1L, "bao"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Chưa khai báo");
        verifyNoInteractions(orderRepo, batchRepo, txRepo);
    }

    @Test
    void donGiaAmKhongGhiDonHayKho() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        form.getItems().get(0).setUnitPrice(new BigDecimal("-1"));
        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(orderRepo, batchRepo, txRepo);
    }
}
