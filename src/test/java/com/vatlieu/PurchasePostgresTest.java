package com.vatlieu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Chạy riêng với PostgreSQL thật; dữ liệu kịch bản tự rollback sau test.
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
@org.springframework.security.test.context.support.WithMockUser(
  username = "suite-owner",
  roles = "OWNER"
)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PurchasePostgresTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  EntityManager entityManager;

  @ParameterizedTest
  @ValueSource(strings = { "", "   ", "  Cô Ánh <A> & B  " })
  void nhapTrucTiepLuuTenNguoiBanCongNoVaTraDu(String seller) throws Exception {
    // Quyền đọc vốn/nợ kiểm tra cả tài khoản trong CSDL, không chỉ vai trò giả lập.
    jdbc.update(
      "INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES ('suite-owner','Quản lý kiểm thử','unused','OWNER',false) ON CONFLICT DO NOTHING"
    );
    long suppliersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM supplier", Long.class);
    String productUrl = mvc
      .perform(
        post("/products")
          .with(csrf())
          .param("productGroup", "AGRICULTURE")
          .param("name", "Hàng nhập trực tiếp " + UUID.randomUUID())
          .param("baseUnit", "kg")
          .param("active", "true")
      )
      .andExpect(status().is3xxRedirection())
      .andReturn()
      .getResponse()
      .getRedirectedUrl();
    long product = Long.parseLong(productUrl.split("/")[2]);
    mvc
      .perform(
        post("/purchases")
          .with(csrf())
          .param("sellerName", seller)
          .param("items[0].productId", String.valueOf(product))
          .param("items[0].qty", "2")
          .param("items[0].unit", "kg")
          .param("items[0].unitPrice", "100")
          .param("paidAmount", "50")
      )
      .andExpect(redirectedUrl("/purchases"));
    entityManager.flush();
    entityManager.clear();
    long order = jdbc.queryForObject(
      "SELECT order_id FROM purchase_item WHERE product_id=?",
      Long.class,
      product
    );
    var saved = jdbc.queryForMap("SELECT * FROM purchase_order WHERE id=?", order);
    assertThat(saved.get("seller_name")).isEqualTo(seller.isBlank() ? null : seller.strip());
    assertThat(saved.get("supplier_id")).isNull();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM supplier", Long.class)).isEqualTo(
      suppliersBefore
    );
    assertDecimal("SELECT qty_on_hand FROM v_stock WHERE product_id=?", product, "2");
    assertDecimal("SELECT total_amount-paid_amount FROM purchase_order WHERE id=?", order, "150");
    String displayName = org.springframework.web.util.HtmlUtils.htmlEscape(
      seller.isBlank() ? "Chưa ghi tên" : seller.strip(),
      "UTF-8"
    );
    for (String path : new String[] { "/purchases", "/stock/lots", "/capital?section=debts" }) {
      mvc
        .perform(get(path))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Người bán: " + displayName)));
    }
    mvc
      .perform(
        get("/capital")
          .param("section", "payments")
          .param("partner", seller.isBlank() ? "Chưa ghi tên" : "cô ánh")
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Người bán: " + displayName)))
      .andExpect(content().string(containsString("phiếu nhập #" + order + ": 50 VNĐ")));
    mvc
      .perform(
        post("/capital/pay")
          .with(csrf())
          .param("type", "purchase")
          .param("id", String.valueOf(order))
          .param("settleAll", "true")
          .param("requestKey", UUID.randomUUID().toString())
      )
      .andExpect(redirectedUrl("/capital?section=debts#supplier-debts"));
    assertDecimal("SELECT paid_amount FROM purchase_order WHERE id=?", order, "200");
    assertDecimal("SELECT SUM(amount) FROM cash_payment WHERE purchase_id=?", order, "200");
    mvc
      .perform(get("/capital?section=debts"))
      .andExpect(
        content().string(
          org.hamcrest.Matchers.not(containsString("id=\"purchase-debt-" + order + "\""))
        )
      );
  }

  @Test
  void loiNhapGiuLaiTenNguoiBanVaKhongLuuPhieu() throws Exception {
    long before = jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Long.class);
    String seller = "Tên " + "A".repeat(151);
    mvc
      .perform(post("/purchases").with(csrf()).param("sellerName", seller))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Tên người bán tối đa 150 ký tự")))
      .andExpect(content().string(containsString("value=\"" + seller + "\"")));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Long.class)).isEqualTo(
      before
    );
  }

  @Test
  void nhapNongSanQuaFormBaCachVaGiuLaiDuLieuSai() throws Exception {
    String url = mvc
      .perform(
        post("/products")
          .with(csrf())
          .param("productGroup", "AGRICULTURE")
          .param("name", "Nông sản biểu mẫu")
          .param("baseUnit", "kg")
          .param("active", "true")
      )
      .andExpect(status().is3xxRedirection())
      .andReturn()
      .getResponse()
      .getRedirectedUrl();
    Long p = Long.valueOf(url.split("/")[2]);
    for (String mode : new String[] { "WEIGHT", "BAGS", "LOT" }) {
      mvc
        .perform(
          post("/purchases")
            .with(csrf())
            .param("items[0].productId", p.toString())
            .param("items[0].unit", "kg")
            .param("items[0].entryMode", mode)
            .param("items[0].qty", "3")
            .param("items[0].unitPrice", "10")
            .param("items[0].bagWeights", "1\n2")
            .param("items[0].lotAmount", "30")
        )
        .andExpect(redirectedUrl("/purchases"));
    }
    entityManager.flush();
    assertDecimal("SELECT qty_on_hand FROM v_stock WHERE product_id=?", p, "9");
    assertDecimal("SELECT SUM(line_amount) FROM purchase_item WHERE product_id=?", p, "90");
    mvc
      .perform(
        post("/purchases")
          .with(csrf())
          .param("items[0].productId", p.toString())
          .param("items[0].entryMode", "BAGS")
          .param("items[0].unit", "kg")
          .param("items[0].bagWeights", "0\n2")
          .param("items[0].unitPrice", "10")
      )
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("0\n2")));
    mvc
      .perform(get(url))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Nông sản")));
  }

  @Test
  void taoSanPhamQuyDoiNhapKhoVaRenderTatCaTrang() throws Exception {
    String categoryName = "Xi măng kiểm thử " + UUID.randomUUID();
    mvc
      .perform(post("/categories").with(csrf()).param("name", categoryName))
      .andExpect(redirectedUrl("/categories"));
    entityManager.flush();
    Long categoryId = jdbc.queryForObject(
      "SELECT id FROM category WHERE name = ?",
      Long.class,
      categoryName
    );

    String editUrl = mvc
      .perform(
        post("/products")
          .with(csrf())
          .param("categoryId", categoryId.toString())
          .param("name", "Xi măng PC40")
          .param("baseUnit", "kg")
          .param("retailPrice", "0")
          .param("minStock", "0")
          .param("active", "true")
      )
      .andExpect(status().is3xxRedirection())
      .andReturn()
      .getResponse()
      .getRedirectedUrl();
    assertThat(editUrl).matches("/products/\\d+/edit");
    Long productId = Long.valueOf(editUrl.split("/")[2]);

    mvc
      .perform(
        post("/products/{id}/units", productId)
          .with(csrf())
          .param("unitName", "bao")
          .param("factor", "50")
      )
      .andExpect(redirectedUrl(editUrl));
    entityManager.flush();
    entityManager.clear();

    // Render cả khi danh sách sản phẩm đã chứa quan hệ Hibernate nạp từ CSDL.
    mvc
      .perform(get("/purchases/new"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Xi măng PC40")))
      .andExpect(content().string(containsString("name=\"productName\"")))
      .andExpect(content().string(containsString("unit-select")));
    mvc
      .perform(get("/outbound/new"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("bao = 50 kg")))
      .andExpect(content().string(containsString("unit-select")));
    mvc
      .perform(
        post("/purchases")
          .with(csrf())
          .param("items[0].productId", productId.toString())
          .param("items[0].qty", "20")
          .param("items[0].unit", "bao")
          .param("items[0].unitPrice", "90000")
          .param("shippingCost", "0")
          .param("paidAmount", "0")
      )
      .andExpect(redirectedUrl("/purchases"));
    entityManager.flush();
    entityManager.clear();

    assertDecimal(
      "SELECT SUM(qty_change) FROM stock_transaction WHERE product_id = ?",
      productId,
      "1000"
    );
    assertDecimal("SELECT qty_on_hand FROM v_stock WHERE product_id = ?", productId, "1000");
    assertDecimal("SELECT avg_cost FROM product WHERE id = ?", productId, "1800");
    assertDecimal("SELECT qty FROM purchase_item WHERE product_id = ?", productId, "1000");
    assertDecimal("SELECT unit_cost FROM purchase_item WHERE product_id = ?", productId, "1800");
    assertDecimal("SELECT qty_left FROM batch WHERE product_id = ?", productId, "1000");
    assertDecimal("SELECT unit_cost FROM batch WHERE product_id = ?", productId, "1800");
    assertDecimal(
      "SELECT unit_cost FROM stock_transaction WHERE product_id = ?",
      productId,
      "1800"
    );
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM stock_transaction WHERE product_id = ? AND tx_type = 'PURCHASE'",
        Long.class,
        productId
      )
    ).isEqualTo(1L);
    assertThat(
      jdbc.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success",
        Long.class
      )
    ).isEqualTo(1L);

    for (String path : new String[] {
      "/categories",
      "/products",
      "/products/new",
      editUrl,
      "/suppliers",
      "/purchases",
      "/purchases/new",
      "/stock",
    }) {
      mvc.perform(get(path)).andExpect(status().isOk());
    }
    mvc
      .perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("href=\"/purchases/new\"")))
      .andExpect(content().string(containsString("href=\"/outbound/new\"")))
      .andExpect(content().string(containsString("href=\"/stock\"")));
    mvc.perform(get("/stock")).andExpect(content().string(containsString("Xi măng PC40")));
  }

  private void assertDecimal(String sql, Long productId, String expected) {
    assertThat(jdbc.queryForObject(sql, BigDecimal.class, productId)).isEqualByComparingTo(
      expected
    );
  }
}
