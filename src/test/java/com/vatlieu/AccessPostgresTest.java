package com.vatlieu;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vatlieu.service.AccountService;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class AccessPostgresTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  jakarta.persistence.EntityManager entityManager;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  AccountService accounts;

  private final String password = "Only-for-tests-123!";

  @BeforeEach
  void users() {
    for (String name : new String[] { "acl-owner", "acl-a", "acl-b" })
      jdbc.update(
        "INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES (?,?,?,?,false)",
        name,
        name,
        encoder.encode(password),
        name.equals("acl-owner") ? "OWNER" : "STAFF"
      );
  }

  private long id(String name) {
    return jdbc.queryForObject("SELECT id FROM app_user WHERE username=?", Long.class, name);
  }

  @Test
  @WithMockUser(username = "acl-owner", roles = "OWNER")
  void supplierCreateUsesSameValidationAsEdit() throws Exception {
    long before = jdbc.queryForObject("SELECT COUNT(*) FROM supplier", Long.class);
    for (String[] values : new String[][] {
      { "   ", "0901234567" },
      { "A".repeat(151), "" },
      { "Người bán", "1".repeat(31) },
    }) {
      mvc
        .perform(post("/suppliers").with(csrf()).param("name", values[0]).param("phone", values[1]))
        .andExpect(redirectedUrl("/suppliers"))
        .andExpect(flash().attributeExists("error"));
    }
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM supplier", Long.class)).isEqualTo(before);
    String name = "Người bán " + UUID.randomUUID();
    mvc
      .perform(
        post("/suppliers")
          .with(csrf())
          .param("name", "  " + name + "  ")
          .param("phone", " 0901234567 ")
      )
      .andExpect(redirectedUrl("/suppliers"))
      .andExpect(flash().attributeExists("msg"));
    assertThat(
      jdbc.queryForObject("SELECT phone FROM supplier WHERE name=?", String.class, name)
    ).isEqualTo("0901234567");
  }

  @Test
  @WithMockUser(username = "acl-owner", roles = "OWNER")
  void unitConversionBelongsToProductAndRejectsInvalidData() throws Exception {
    long material = jdbc.queryForObject(
      "INSERT INTO product(name,base_unit,product_group) VALUES ('Vật liệu kiểm thử','kg','MATERIAL') RETURNING id",
      Long.class
    );
    long agriculture = jdbc.queryForObject(
      "INSERT INTO product(name,base_unit,product_group) VALUES ('Nông sản kiểm thử','kg','AGRICULTURE') RETURNING id",
      Long.class
    );
    String materialUrl = "/products/" + material;
    for (String[] values : new String[][] {
      { "bao", "0" },
      { "bao", "-1" },
      { " ", "50" },
      { "x".repeat(21), "50" },
    }) {
      mvc
        .perform(
          post(materialUrl + "/units")
            .with(csrf())
            .param("unitName", values[0])
            .param("factor", values[1])
        )
        .andExpect(redirectedUrl(materialUrl + "/edit"))
        .andExpect(flash().attributeExists("error"));
    }
    mvc
      .perform(
        post(materialUrl + "/units")
          .with(csrf())
          .param("unitName", " bao ")
          .param("factor", "50.5")
      )
      .andExpect(flash().attributeExists("msg"));
    long conversion = jdbc.queryForObject(
      "SELECT id FROM unit_conversion WHERE product_id=? AND unit_name='bao'",
      Long.class,
      material
    );
    assertThat(
      jdbc.queryForObject(
        "SELECT factor FROM unit_conversion WHERE id=?",
        java.math.BigDecimal.class,
        conversion
      )
    ).isEqualByComparingTo("50.5");
    mvc
      .perform(
        post(materialUrl + "/units")
          .with(csrf())
          .param("unitName", "BAO")
          .param("factor", "40")
      )
      .andExpect(flash().attributeExists("error"));
    mvc
      .perform(
        post("/products/" + agriculture + "/units")
          .with(csrf())
          .param("unitName", "bao")
          .param("factor", "50")
      )
      .andExpect(flash().attributeExists("error"));
    mvc
      .perform(post("/products/" + agriculture + "/units/" + conversion + "/delete").with(csrf()))
      .andExpect(flash().attributeExists("error"));
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM unit_conversion WHERE id=?", Long.class, conversion)
    ).isEqualTo(1);
    mvc
      .perform(post(materialUrl + "/units/" + conversion + "/delete").with(csrf()))
      .andExpect(flash().attributeExists("msg"));
    // Test bao ngoài bằng transaction; đẩy lệnh JPA xuống trước khi kiểm tra bằng JDBC.
    entityManager.flush();
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM unit_conversion WHERE id=?", Long.class, conversion)
    ).isZero();
  }

  private long order(String name, String note, String kind) {
    return jdbc.queryForObject(
      "INSERT INTO outbound_order(kind,customer,note,total_amount,cost_amount,request_key,created_by) VALUES (?,'Khách',?,100,73,?,?) RETURNING id",
      Long.class,
      kind,
      note,
      UUID.randomUUID(),
      name == null ? null : id(name)
    );
  }

  @Test
  void anonymousMustLoginAndNoRegistration() throws Exception {
    mvc
      .perform(get("/stock"))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("**/login"));
    mvc
      .perform(get("/login"))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("/register"))))
      .andExpect(header().string("Referrer-Policy", "no-referrer"))
      .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self';")))
      .andExpect(header().string("Content-Security-Policy", not(containsString("recaptcha"))))
      .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self'")));
    for (String path : new String[] { "/register", "/register/verify", "/register/details" }) {
      mvc.perform(get(path)).andExpect(redirectedUrlPattern("**/login"));
      mvc
        .perform(post(path).with(user("acl-owner").roles("OWNER")).with(csrf()))
        .andExpect(status().isForbidden());
    }
    mvc
      .perform(post("/admin/users").with(csrf()).param("username", "hacker"))
      .andExpect(status().is3xxRedirection());
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE username='hacker'", Integer.class)
    ).isZero();
  }

  @Test
  @WithMockUser(username = "acl-a", roles = "STAFF")
  void staffSharesBusinessScreensAndSalesHistory() throws Exception {
    jdbc.update("INSERT INTO product(name,base_unit) VALUES ('Hàng có giá vốn','kg')");
    mvc
      .perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("href=\"/purchases/new\"")))
      .andExpect(content().string(containsString("href=\"/capital\"")))
      .andExpect(content().string(containsString("href=\"/products\"")))
      .andExpect(content().string(not(containsString("href=\"/admin/users\""))));
    long other = order("acl-b", "Phiếu chung", "SALE");
    long legacy = order(null, "Phiếu cũ", "SALE");
    long waste = order("acl-owner", "Hao hụt chung", "WASTE");
    long trashed = order("acl-b", "Phiếu đã xóa", "SALE");
    jdbc.update("UPDATE outbound_order SET trashed=true WHERE id=?", trashed);
    mvc
      .perform(get("/outbound"))
      .andExpect(content().string(containsString("/outbound/" + other + "\"")))
      .andExpect(content().string(not(containsString("/outbound/" + trashed + "\""))));
    mvc
      .perform(get("/outbound").param("trash", "true"))
      .andExpect(content().string(containsString("/outbound/" + trashed + "\"")));
    for (long receipt : new long[] { other, legacy, waste, trashed })
      mvc
        .perform(get("/outbound/" + receipt))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Tổng giá vốn")));
    for (String path : new String[] {
      "/capital",
      "/capital?section=debts",
      "/stock/lots",
      "/purchases",
      "/purchases/new",
      "/products/new",
      "/categories",
      "/suppliers",
      "/outbound/new?kind=WASTE",
    })
      mvc.perform(get(path)).andExpect(status().isOk());
    mvc.perform(get("/stock")).andExpect(content().string(containsString("Giá vốn")));
    mvc
      .perform(post("/categories").with(csrf()).param("name", "Danh mục chung"))
      .andExpect(flash().attributeExists("msg"));
  }

  @Test
  @WithMockUser(username = "acl-a", roles = "STAFF")
  void staffCannotAdministerAccountsEvenWithValidCsrf() throws Exception {
    long other = id("acl-b");
    assertThatThrownBy(() -> accounts.deleteStaff(other)).isInstanceOf(
      org.springframework.security.access.AccessDeniedException.class
    );
    assertThatThrownBy(() -> accounts.resetPassword(other, password)).isInstanceOf(
      org.springframework.security.access.AccessDeniedException.class
    );
    var before = jdbc.queryForMap("SELECT * FROM app_user WHERE id=?", other);
    mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    mvc
      .perform(
        post("/admin/users")
          .with(csrf())
          .param("username", "acl-extra")
          .param("role", "OWNER")
          .param("fullName", "Mới")
          .param("password", password)
      )
      .andExpect(status().isForbidden());
    for (String op : new String[] { "password", "active", "delete" })
      mvc
        .perform(
          post("/admin/users/" + other + "/" + op)
            .with(csrf())
            .param("password", password)
            .param("active", "false")
        )
        .andExpect(status().isForbidden());
    assertThat(jdbc.queryForMap("SELECT * FROM app_user WHERE id=?", other)).isEqualTo(before);
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE username='acl-extra'", Integer.class)
    ).isZero();
    mvc.perform(get("/account/password")).andExpect(status().isOk());
  }

  private MockHttpSession loginAs(String name, String secret) throws Exception {
    return (MockHttpSession) mvc
      .perform(post("/login").with(csrf()).param("username", name).param("password", secret))
      .andExpect(redirectedUrl("/"))
      .andReturn()
      .getRequest()
      .getSession(false);
  }

  @Test
  void losingManagerRoleExpiresExistingManagerSession() throws Exception {
    MockHttpSession session = loginAs("acl-owner", password);
    jdbc.update("UPDATE app_user SET role='STAFF' WHERE username='acl-owner'");
    mvc.perform(get("/admin/users").session(session)).andExpect(redirectedUrl("/login?expired"));
    mvc
      .perform(get("/admin/users").session(loginAs("acl-owner", password)))
      .andExpect(status().isForbidden());
  }

  @Test
  void managerResetAndDeletionRevokeSessionsAndPreserveCreator() throws Exception {
    long staff = id("acl-a");
    long receipt = order("acl-a", "Phiếu lưu", "SALE");
    String prefix = "/admin/users/" + staff;
    MockHttpSession staffSession = loginAs("acl-a", password);
    MockHttpSession manager = loginAs("acl-owner", password);
    String temporary = "New-test-only-456!";
    mvc
      .perform(
        post(prefix + "/password")
          .session(manager)
          .with(csrf())
          .param("password", temporary)
      )
      .andExpect(flash().attributeExists("msg"));
    mvc.perform(get("/stock").session(staffSession)).andExpect(redirectedUrl("/login?expired"));
    staffSession = loginAs("acl-a", temporary);
    mvc.perform(get("/stock").session(staffSession)).andExpect(redirectedUrl("/account/password"));
    mvc.perform(post(prefix + "/delete").session(manager)).andExpect(status().isForbidden());
    mvc
      .perform(
        post(prefix + "/delete")
          .session(manager)
          .with(csrf())
      )
      .andExpect(flash().attributeExists("msg"));
    mvc
      .perform(get("/account/password").session(staffSession))
      .andExpect(redirectedUrl("/login?expired"));
    mvc
      .perform(post("/login").with(csrf()).param("username", "acl-a").param("password", temporary))
      .andExpect(redirectedUrl("/login?error"));
    mvc
      .perform(get("/admin/users").session(manager))
      .andExpect(content().string(not(containsString("(acl-a)"))));
    mvc
      .perform(
        post(prefix + "/active")
          .session(manager)
          .with(csrf())
          .param("active", "true")
      )
      .andExpect(flash().attributeExists("error"));
    mvc
      .perform(
        post(prefix + "/password")
          .session(manager)
          .with(csrf())
          .param("password", temporary)
      )
      .andExpect(flash().attributeExists("error"));
    assertThat(
      jdbc.queryForObject("SELECT created_by FROM outbound_order WHERE id=?", Long.class, receipt)
    ).isEqualTo(staff);
  }

  @Test
  @WithMockUser(username = "acl-owner", roles = "OWNER")
  void accountAdministrationCannotTargetAnyManager() throws Exception {
    accounts.create("acl-owner2", "Quản lý khác", "OWNER", password);
    for (String name : new String[] { "acl-owner", "acl-owner2" }) {
      String prefix = "/admin/users/" + id(name);
      for (String op : new String[] { "delete", "active", "password" }) {
        mvc
          .perform(
            post(prefix + "/" + op)
              .with(csrf())
              .param("active", "false")
              .param("password", password)
          )
          .andExpect(flash().attributeExists("error"));
      }
      assertThat(
        jdbc.queryForObject(
          "SELECT active AND deleted_at IS NULL AND auth_version=0 FROM app_user WHERE username=?",
          Boolean.class,
          name
        )
      ).isTrue();
    }
  }

  @Test
  @WithMockUser(username = "acl-a", roles = "STAFF")
  void saleUsesAuthenticatedCreatorAndCsrf() throws Exception {
    long c = jdbc.queryForObject(
      "INSERT INTO category(name) VALUES (?) RETURNING id",
      Long.class,
      "ACL " + UUID.randomUUID()
    );
    long p = jdbc.queryForObject(
      "INSERT INTO product(category_id,name,base_unit) VALUES (?,'ACL hàng','kg') RETURNING id",
      Long.class,
      c
    );
    long b = jdbc.queryForObject(
      "INSERT INTO batch(product_id,qty_in,qty_left,unit_cost) VALUES (?,2,2,73) RETURNING id",
      Long.class,
      p
    );
    jdbc.update(
      "INSERT INTO stock_transaction(product_id,batch_id,tx_type,qty_change,unit_cost) VALUES (?,?,'PURCHASE',2,73)",
      p,
      b
    );
    mvc.perform(post("/outbound").param("kind", "SALE")).andExpect(status().isForbidden());
    String redirect = mvc
      .perform(
        post("/outbound")
          .with(csrf())
          .param("kind", "SALE")
          .param("createdBy", Long.toString(id("acl-b")))
          .param("items[0].productId", Long.toString(p))
          .param("items[0].qty", "1")
          .param("items[0].unit", "kg")
          .param("items[0].unitPrice", "100")
          .param("paidAmount", "100")
      )
      .andExpect(status().is3xxRedirection())
      .andReturn()
      .getResponse()
      .getRedirectedUrl();
    long sale = Long.parseLong(redirect.substring(redirect.lastIndexOf('/') + 1));
    assertThat(
      jdbc.queryForObject("SELECT created_by FROM outbound_order WHERE id=?", Long.class, sale)
    ).isEqualTo(id("acl-a"));
  }

  @Test
  @WithMockUser(username = "acl-owner", roles = "OWNER")
  void managerCategoryCrudAndProtection() throws Exception {
    mvc
      .perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("href=\"/products\"")))
      .andExpect(content().string(containsString("href=\"/admin/users\"")));
    String name = "Danh mục " + UUID.randomUUID();
    mvc
      .perform(post("/categories").with(csrf()).param("name", name))
      .andExpect(redirectedUrl("/categories"));
    long c = jdbc.queryForObject("SELECT id FROM category WHERE name=?", Long.class, name);
    mvc
      .perform(
        post("/categories/" + c + "/edit")
          .with(csrf())
          .param("name", name + " sửa")
          .param("perishable", "true")
      )
      .andExpect(redirectedUrl("/categories"));
    assertThat(
      jdbc.queryForObject("SELECT perishable FROM category WHERE id=?", Boolean.class, c)
    ).isTrue();
    jdbc.update("INSERT INTO product(category_id,name,base_unit) VALUES (?,'Hàng','kg')", c);
    mvc
      .perform(post("/categories/" + c + "/delete").with(csrf()))
      .andExpect(flash().attribute("error", containsString("đang có sản phẩm")));
    long empty = jdbc.queryForObject(
      "INSERT INTO category(name) VALUES (?) RETURNING id",
      Long.class,
      "Trống " + UUID.randomUUID()
    );
    mvc
      .perform(post("/categories/" + empty + "/delete").with(csrf()))
      .andExpect(flash().attribute("msg", "Đã xóa danh mục"));
    assertThat(
      jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id=?", Integer.class, empty)
    ).isZero();
    mvc
      .perform(get("/categories"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Sửa danh mục")));
    mvc.perform(get("/admin/users")).andExpect(status().isOk());
    mvc
      .perform(get("/purchases/new"))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("items[1].productId"))));
  }

  @Test
  @WithMockUser(username = "acl-owner", roles = "OWNER")
  void passwordsHashedAndSelfLockDenied() {
    accounts.create("acl-new", "Người mới", "STAFF", password);
    String hash = jdbc.queryForObject(
      "SELECT password FROM app_user WHERE username='acl-new'",
      String.class
    );
    assertThat(hash).isNotEqualTo(password);
    assertThat(encoder.matches(password, hash)).isTrue();
    assertThatThrownBy(() -> accounts.setActive(id("acl-owner"), false)).isInstanceOf(
      IllegalArgumentException.class
    );
    assertThatThrownBy(() -> accounts.create("acl-other", "Tên", "ROOT", password)).isInstanceOf(
      IllegalArgumentException.class
    );
  }

  @Test
  @WithMockUser(username = "acl-owner", roles = "OWNER")
  void phoneLoginAcceptsDomesticAndInternationalFormsForTheSameAccount() throws Exception {
    accounts.create("+84 912 345 678", "Nhân viên thử", "STAFF", password);
    assertThatThrownBy(() ->
      accounts.create("0912345678", "Trùng số", "STAFF", password)
    ).isInstanceOf(IllegalArgumentException.class);
    for (String login : new String[] { "0912345678", "+84 912 345 678" }) {
      mvc
        .perform(post("/login").with(csrf()).param("username", login).param("password", password))
        .andExpect(redirectedUrl("/"));
    }
  }

  @Test
  void realLoginAndSessionRevocation() throws Exception {
    mvc
      .perform(post("/login").with(csrf()).param("username", "acl-a").param("password", "wrong"))
      .andExpect(redirectedUrl("/login?error"));
    MockHttpSession session = (MockHttpSession) mvc
      .perform(post("/login").with(csrf()).param("username", "acl-a").param("password", password))
      .andExpect(redirectedUrl("/"))
      .andReturn()
      .getRequest()
      .getSession(false);
    mvc.perform(get("/").session(session)).andExpect(status().isOk());
    jdbc.update(
      "UPDATE app_user SET active=false,auth_version=auth_version+1 WHERE username='acl-a'"
    );
    mvc.perform(get("/stock").session(session)).andExpect(redirectedUrl("/login?expired"));
    mvc
      .perform(post("/login").with(csrf()).param("username", "acl-a").param("password", password))
      .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void firstLoginMustChangePassword() throws Exception {
    jdbc.update("UPDATE app_user SET must_change_password=true WHERE username='acl-a'");
    MockHttpSession session = (MockHttpSession) mvc
      .perform(post("/login").with(csrf()).param("username", "acl-a").param("password", password))
      .andReturn()
      .getRequest()
      .getSession(false);
    mvc.perform(get("/stock").session(session)).andExpect(redirectedUrl("/account/password"));
    mvc.perform(get("/account/password").session(session)).andExpect(status().isOk());
    mvc
      .perform(
        post("/account/password")
          .session(session)
          .with(csrf())
          .param("oldPassword", password)
          .param("newPassword", "New-test-password-456!")
          .param("confirm", "New-test-password-456!")
      )
      .andExpect(redirectedUrl("/login?changed"));
    assertThat(
      jdbc.queryForObject(
        "SELECT must_change_password FROM app_user WHERE username='acl-a'",
        Boolean.class
      )
    ).isFalse();
  }
}
