package com.vatlieu;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vatlieu.form.*;
import com.vatlieu.service.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "direct-suite-owner", roles = "OWNER")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class DirectPurchasePostgresTest {

  @Autowired
  PurchaseService purchases;

  @Autowired
  OperationsService operations;

  @Autowired
  StockService stock;

  @Autowired
  PurchaseTrashService trash;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  EntityManager em;

  @Autowired
  MockMvc mvc;

  @Autowired
  PlatformTransactionManager transactions;

  @BeforeEach
  void actor() {
    jdbc.update(
      "INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES ('direct-suite-owner','Kiểm thử','unused','OWNER',false)"
    );
  }

  private PurchaseForm form(String name, String qty, String price) {
    var form = new PurchaseForm();
    form.setDirectEntry(true);
    form.setProductName(name);
    form.setProductGroup("AGRICULTURE");
    form.setBaseUnit("kg");
    var item = new PurchaseItemForm();
    item.setQty(new BigDecimal(qty));
    item.setUnitPrice(new BigDecimal(price));
    form.getItems().add(item);
    return form;
  }

  @Test
  void nhapHaiLoCungHangBanTheoLoVaTinhDungVon() throws Exception {
    String name = "Sắn tươi " + UUID.randomUUID();
    var first = form("  " + name + "  ", "10", "100");
    first.setSellerName("Cô Ánh");
    first.setPaidAmount(new BigDecimal("200"));
    first.setLotCode("san-" + UUID.randomUUID().toString().substring(0, 8));
    var order1 = purchases.create(first);
    var second = form(name.toLowerCase(java.util.Locale.ROOT), "5", "200");
    second.setSellerName("Chú Bình");
    second.setSettleAll(true);
    var order2 = purchases.create(second);
    em.flush();
    long product = order1.getItems().get(0).getProduct().getId();
    assertThat(order2.getItems().get(0).getProduct().getId()).isEqualTo(product);
    assertThat(order1.getLotCode()).isEqualTo(
      first.getLotCode().toUpperCase(java.util.Locale.ROOT)
    );
    assertThat(order2.getLotCode()).startsWith("NH-").isNotEqualTo(order1.getLotCode());
    assertThat(order1.getSupplier()).isNull();
    assertThat(stock.findAll()).anySatisfy(row -> {
      assertThat(row.productId()).isEqualTo(product);
      assertThat(row.qtyOnHand()).isEqualByComparingTo("15");
      assertThat(row.value()).isEqualByComparingTo("2000");
    });
    Long batch = jdbc.queryForObject(
      "SELECT b.id FROM batch b JOIN purchase_item i ON i.id=b.purchase_item_id WHERE i.order_id=?",
      Long.class,
      order1.getId()
    );
    var sale = new OutboundForm();
    sale.setCustomer("Anh Tuấn");
    sale.setPaidAmount(new BigDecimal("600"));
    var item = new PurchaseItemForm();
    item.setProductId(product);
    item.setBatchId(batch);
    item.setUnit("kg");
    item.setQty(new BigDecimal("4"));
    item.setUnitPrice(new BigDecimal("250"));
    sale.getItems().add(item);
    long saleId = operations.create(sale);
    var detail = operations.detail(saleId);
    assertThat((BigDecimal) detail.get("total_amount")).isEqualByComparingTo("1000");
    assertThat((BigDecimal) detail.get("cost_amount")).isEqualByComparingTo("400");
    assertThat(stock.findAll()).anySatisfy(row -> {
      assertThat(row.productId()).isEqualTo(product);
      assertThat(row.qtyOnHand()).isEqualByComparingTo("11");
      assertThat(row.value()).isEqualByComparingTo("1600");
    });
    assertThat(purchases.history("anh", false))
      .extracting(com.vatlieu.entity.PurchaseOrder::getId)
      .contains(order1.getId())
      .doesNotContain(order2.getId());
    assertThat(operations.orders("tuan")).anySatisfy(row ->
      assertThat(((Number) row.get("id")).longValue()).isEqualTo(saleId)
    );
    mvc
      .perform(get("/purchases").param("keyword", order1.getLotCode()))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Cô Ánh")));
    mvc
      .perform(get("/outbound/" + saleId))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(order1.getLotCode())));
    mvc
      .perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Vốn trong hàng còn")));
  }

  @Test
  void chuyenPhieuVaoThungRacXemLaiVaKhoiPhuc() throws Exception {
    var entry = form("Ngô thử " + UUID.randomUUID(), "6.25", "1200");
    entry.setSellerName("Cô Ánh");
    var order = purchases.create(entry);
    em.flush();
    String path = "/purchases/" + order.getId();
    String code = order.getLotCode();
    mvc.perform(post(path + "/trash")).andExpect(status().isForbidden());
    mvc
      .perform(get("/purchases").param("keyword", code))
      .andExpect(content().string(containsString("Chuyển phiếu nhập #" + order.getId())))
      .andExpect(content().string(containsString(code)));
    for (int i = 0; i < 2; i++) {
      mvc.perform(post(path + "/trash").with(csrf())).andExpect(redirectedUrl("/purchases"));
    }
    mvc
      .perform(get("/purchases").param("keyword", "co anh"))
      .andExpect(content().string(org.hamcrest.Matchers.not(containsString(code))));
    mvc
      .perform(get("/purchases").param("trash", "true").param("keyword", "co anh"))
      .andExpect(content().string(containsString(code)))
      .andExpect(content().string(containsString("Khôi phục phiếu")))
      .andExpect(content().string(containsString("7.500 VNĐ")));
    mvc
      .perform(get("/purchases").param("trash", "true").param("keyword", "khong trung"))
      .andExpect(content().string(org.hamcrest.Matchers.not(containsString(code))));
    for (int i = 0; i < 2; i++) {
      mvc
        .perform(post(path + "/restore").with(csrf()))
        .andExpect(redirectedUrl("/purchases?trash=true"));
    }
    mvc
      .perform(get("/purchases").param("keyword", code))
      .andExpect(content().string(containsString(code)));
    mvc
      .perform(get("/purchases").param("trash", "true").param("keyword", code))
      .andExpect(content().string(containsString("Không tìm thấy phiếu nhập phù hợp.")));
  }

  @Test
  void xoaVaKhoiPhucDungLoVonVaNoSauKhiDaNhapLoKhac() {
    String name = "Sắn " + UUID.randomUUID();
    var first = form(name, "4.125", "1000");
    first.setShippingCost(new BigDecimal("825"));
    var baseline = operations.summary();
    var order = purchases.create(first);
    em.flush();
    long id = order.getId();
    long product = order.getItems().get(0).getProduct().getId();
    trash.setTrashed(id, true);
    trash.setTrashed(id, true);
    var afterTrash = operations.summary();
    baseline.forEach((key, value) ->
      assertThat((BigDecimal) afterTrash.get(key)).isEqualByComparingTo((BigDecimal) value)
    );
    assertThat(stock.findAll())
      .filteredOn(r -> r.productId() == product)
      .singleElement()
      .satisfies(r -> {
        assertThat(r.qtyOnHand()).isZero();
        assertThat(r.avgCost()).isZero();
      });
    assertThat(operations.debts()).noneMatch(
      d -> d.get("type").equals("purchase") && d.get("id").equals(id)
    );
    assertThat(stock.lots(true)).noneMatch(b -> Long.valueOf(id).equals(b.get("purchase_id")));
    assertThatThrownBy(() -> operations.pay("purchase", id, BigDecimal.ONE, UUID.randomUUID(), ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Thùng rác");
    em.clear();
    purchases.create(form(name, "4.125", "2000"));
    em.flush();
    trash.setTrashed(id, false);
    trash.setTrashed(id, false);
    assertThat(stock.findAll())
      .filteredOn(r -> r.productId() == product)
      .singleElement()
      .satisfies(r -> {
        assertThat(r.qtyOnHand()).isEqualByComparingTo("8.25");
        assertThat(r.value()).isEqualByComparingTo("13200");
        assertThat(r.avgCost()).isEqualByComparingTo("1600");
      });
    assertThat((BigDecimal) operations.summary().get("payable")).isEqualByComparingTo(
      ((BigDecimal) baseline.get("payable")).add(new BigDecimal("13200"))
    );
    assertThat(operations.debts()).anyMatch(
      d -> d.get("type").equals("purchase") && d.get("id").equals(id)
    );
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM stock_transaction WHERE tx_type='ADJUSTMENT' AND ref_id=?",
        Long.class,
        id
      )
    ).isEqualTo(2);
  }

  @Test
  void chanXoaPhieuDaTraDaBanHoacDaHaoHut() {
    var paid = form("Lô đã trả " + UUID.randomUUID(), "3", "10");
    paid.setPaidAmount(BigDecimal.ONE);
    var paidOrder = purchases.create(paid);
    em.flush();
    assertThatThrownBy(() -> trash.setTrashed(paidOrder.getId(), true)).hasMessageContaining(
      "đã trả tiền"
    );
    for (String kind : java.util.List.of("SALE", "WASTE")) {
      var order = purchases.create(form("Lô " + UUID.randomUUID(), "3", "10"));
      em.flush();
      var sale = new OutboundForm();
      sale.setKind(kind);
      sale.setNote("Kiểm thử");
      sale.setCustomer("Khách thử");
      var row = new PurchaseItemForm();
      row.setProductId(order.getItems().get(0).getProduct().getId());
      row.setQty("SALE".equals(kind) ? new BigDecimal("3") : BigDecimal.ONE);
      row.setUnitPrice(BigDecimal.TEN);
      sale.getItems().add(row);
      operations.create(sale);
      var before = operations.summary();
      assertThatThrownBy(() -> trash.setTrashed(order.getId(), true)).hasMessageContaining(
        "xuất bán hoặc ghi hao hụt"
      );
      assertThat(purchases.history(order.getLotCode(), true)).isEmpty();
      assertThat(operations.summary()).isEqualTo(before);
    }
  }

  @Test
  void bamLuuLapKhongNhanDoiHangHayTien() throws Exception {
    String name = "Ngô " + UUID.randomUUID();
    String key = UUID.randomUUID().toString();
    String code = purchases.nextLotCode();
    for (int repeat = 0; repeat < 2; repeat++) {
      mvc
        .perform(
          post("/purchases")
            .with(csrf())
            .param("directEntry", "true")
            .param("requestKey", key)
            .param("productName", name)
            .param("productGroup", "AGRICULTURE")
            .param("lotCode", code)
            .param("sellerName", "")
            .param("items[0].qty", "0.005")
            .param("items[0].unitPrice", "101")
            .param("settleAll", "true")
        )
        .andExpect(redirectedUrl("/purchases"));
    }
    em.flush();
    long order = jdbc.queryForObject(
      "SELECT id FROM purchase_order WHERE request_key=?",
      Long.class,
      UUID.fromString(key)
    );
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE name=?", Long.class, name)
    ).isEqualTo(1);
    assertThat(
      jdbc.queryForObject(
        "SELECT SUM(qty_change) FROM stock_transaction WHERE ref_id=? AND tx_type='PURCHASE'",
        BigDecimal.class,
        order
      )
    ).isEqualByComparingTo("0.005");
    assertThat(
      jdbc.queryForObject(
        "SELECT SUM(amount) FROM cash_payment WHERE purchase_id=?",
        BigDecimal.class,
        order
      )
    ).isEqualByComparingTo("1");
  }

  @Test
  void maLoTrungBiChanVaGiuTenDaNhap() throws Exception {
    var first = form("Gạo " + UUID.randomUUID(), "1", "10");
    first.setLotCode("Trung-" + UUID.randomUUID().toString().substring(0, 8));
    purchases.create(first);
    em.flush();
    String newName = "Hàng không được tạo " + UUID.randomUUID();
    mvc
      .perform(
        post("/purchases")
          .with(csrf())
          .param("directEntry", "true")
          .param("productName", newName)
          .param("lotCode", first.getLotCode())
          .param("items[0].qty", "1")
          .param("items[0].unitPrice", "10")
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Mã lô đã được dùng")))
      .andExpect(content().string(containsString(newName)));
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE name=?", Long.class, newName)
    ).isZero();
  }

  @Test
  void nhapSaiTienRollbackCaTenHangMoi() {
    String name = "Không lưu " + UUID.randomUUID();
    var invalid = form(name, "2", "10");
    invalid.setPaidAmount(new BigDecimal("21"));
    var tx = new TransactionTemplate(transactions);
    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThatThrownBy(() -> tx.execute(status -> purchases.create(invalid))).isInstanceOf(
      IllegalArgumentException.class
    );
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE name=?", Long.class, name)
    ).isZero();
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM purchase_order WHERE request_key=?",
        Long.class,
        invalid.getRequestKey()
      )
    ).isZero();
  }

  @Test
  void chiMotMatHangVaKhongTuKhoiPhucHangDaXoa() {
    var multi = form("Hai hàng", "1", "10");
    multi.getItems().add(new PurchaseItemForm());
    assertThatThrownBy(() -> purchases.create(multi))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("một mặt hàng");
    String name = "Hàng đã xóa " + UUID.randomUUID();
    jdbc.update(
      "INSERT INTO product(name,base_unit,product_group,active) VALUES (?,'kg','AGRICULTURE',false)",
      name
    );
    assertThatThrownBy(() -> purchases.create(form(name, "1", "10")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("ngừng sử dụng");
  }
}
