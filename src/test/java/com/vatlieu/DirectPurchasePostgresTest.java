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
    assertThat(purchases.history("anh"))
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
