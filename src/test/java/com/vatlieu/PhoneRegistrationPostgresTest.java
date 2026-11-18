package com.vatlieu;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vatlieu.service.PhoneRegistrationService;
import com.vatlieu.service.PhoneVerification;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PhoneRegistrationPostgresTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  PhoneRegistrationService registrations;

  @MockBean
  PhoneVerification sms;

  private static final String PHONE = "0981234567";
  private static final String SID = "firebase-session-".repeat(8);
  private static final String CAPTCHA = "test-captcha";
  private static final String PASSWORD = "Phone-registration-test!";

  @BeforeEach
  void setup() {
    jdbc.update(
      "INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES ('phone-reviewer','Quản lý','unused','OWNER',false)"
    );
    when(sms.configured()).thenReturn(true);
    when(sms.captchaSiteKey()).thenReturn("test-public-site-key");
    when(sms.send(anyString(), eq(CAPTCHA))).thenReturn(SID);
    when(sms.approved(SID, PHONE, "123456")).thenReturn(true);
  }

  @Test
  void verifiedStaffMustBeApprovedBeforeLogin() throws Exception {
    mvc
      .perform(get("/register"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Nhận mã qua SMS")));
    var session = new MockHttpSession();
    mvc
      .perform(
        post("/register")
          .with(csrf())
          .session(session)
          .param("phone", "+84 981 234 567")
          .param("g-recaptcha-response", CAPTCHA)
      )
      .andExpect(redirectedUrl("/register/verify"));
    mvc
      .perform(get("/register/verify").session(session))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(PHONE)));
    mvc
      .perform(
        post("/register/verify")
          .with(csrf())
          .session(session)
          .param("code", "123456")
          .param("fullName", "Người mới")
          .param("password", PASSWORD)
      )
      .andExpect(redirectedUrl("/login?registered"));
    assertThat(
      jdbc.queryForObject(
        "SELECT role='STAFF' AND NOT active AND pending_approval AND NOT must_change_password FROM app_user WHERE username=?",
        Boolean.class,
        PHONE
      )
    ).isTrue();
    mvc
      .perform(post("/login").with(csrf()).param("username", PHONE).param("password", PASSWORD))
      .andExpect(redirectedUrl("/login?error"));
    long id = jdbc.queryForObject("SELECT id FROM app_user WHERE username=?", Long.class, PHONE);
    mvc
      .perform(
        post("/admin/users/" + id + "/active")
          .with(user("phone-reviewer").roles("OWNER"))
          .with(csrf())
          .param("active", "true")
      )
      .andExpect(redirectedUrl("/admin/users"));
    mvc
      .perform(
        post("/login").with(csrf()).param("username", "+84981234567").param("password", PASSWORD)
      )
      .andExpect(redirectedUrl("/"));
    mvc.perform(get("/register/verify").session(session)).andExpect(redirectedUrl("/register"));
  }

  @Test
  void sendLimitsAndExpiryAreEnforcedWithoutCreatingAccount() throws Exception {
    var session = new MockHttpSession();
    mvc
      .perform(
        post("/register")
          .with(csrf())
          .session(session)
          .param("phone", PHONE)
          .param("g-recaptcha-response", CAPTCHA)
      )
      .andExpect(redirectedUrl("/register/verify"));
    mvc
      .perform(
        post("/register").with(csrf()).param("phone", PHONE).param("g-recaptcha-response", CAPTCHA)
      )
      .andExpect(model().attributeExists("error"));
    verify(sms, times(1)).send(PHONE, CAPTCHA);
    for (int i = 0; i < 6; i++) {
      mvc
        .perform(
          post("/register/verify")
            .with(csrf())
            .session(session)
            .param("code", "000000")
            .param("fullName", "Tên thử")
            .param("password", PASSWORD)
        )
        .andExpect(model().attributeExists("error"));
    }
    verify(sms, times(5)).approved(SID, PHONE, "000000");
    jdbc.update(
      "UPDATE registration_sms SET attempts=0,requested_at=now()-interval '11 minutes' WHERE phone=?",
      PHONE
    );
    mvc
      .perform(
        post("/register/verify")
          .with(csrf())
          .session(session)
          .param("code", "123456")
          .param("fullName", "Tên thử")
          .param("password", PASSWORD)
      )
      .andExpect(model().attributeExists("error"));
    verify(sms, never()).approved(SID, PHONE, "123456");
    assertThat(
      jdbc.queryForObject("SELECT count(*) FROM app_user WHERE username=?", Integer.class, PHONE)
    ).isZero();
  }

  @Test
  void unconfiguredSmsAndInvalidPhonesCannotStartRegistration() throws Exception {
    mvc
      .perform(post("/register").with(csrf()).param("phone", "12345"))
      .andExpect(model().attributeExists("error"));
    when(sms.configured()).thenReturn(false);
    mvc
      .perform(get("/register"))
      .andExpect(content().string(containsString("Cửa hàng chưa bật gửi mã SMS")));
    mvc
      .perform(post("/register").with(csrf()).param("phone", PHONE))
      .andExpect(model().attributeExists("error"));
    mvc.perform(get("/register/verify")).andExpect(redirectedUrl("/register"));
    verify(sms, never()).send(anyString(), anyString());
  }

  @Test
  void incompleteCaptchaAndExistingAccountsDoNotSendSms() {
    assertThatThrownBy(() -> registrations.start(PHONE, "")).isInstanceOf(
      IllegalArgumentException.class
    );
    jdbc.update(
      "INSERT INTO app_user(username,full_name,password) VALUES (?,'Người đã có','unused')",
      PHONE
    );
    registrations.start(PHONE, CAPTCHA);
    assertThat(
      jdbc.queryForObject(
        "SELECT count(*) FROM registration_sms WHERE phone=?",
        Integer.class,
        PHONE
      )
    ).isZero();
    verify(sms, never()).send(anyString(), anyString());
  }

  @Test
  void uncertainDeliveryKeepsItsSlotAndDoesNotRetry() {
    when(sms.send(PHONE, CAPTCHA)).thenThrow(new IllegalArgumentException("Mất kết nối"));
    assertThatThrownBy(() -> registrations.start(PHONE, CAPTCHA)).hasMessage("Mất kết nối");
    assertThat(
      jdbc.queryForObject(
        "SELECT count(*) FROM registration_sms WHERE phone=? AND firebase_session_info IS NULL",
        Integer.class,
        PHONE
      )
    ).isOne();
    assertThatThrownBy(() -> registrations.start(PHONE, CAPTCHA)).hasMessageContaining("60 giây");
    verify(sms, times(1)).send(PHONE, CAPTCHA);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void resendAndTwoSimultaneousRequestsShareTheLastOfTenSlots() throws Exception {
    var threads = Executors.newFixedThreadPool(2);
    try {
      for (int i = 0; i < 8; i++) registrations.start("098111220" + i, CAPTCHA);
      jdbc.update(
        "UPDATE registration_sms SET requested_at=now()-interval '61 seconds' WHERE phone='0981112200'"
      );
      registrations.start("0981112200", CAPTCHA);
      var ready = new CyclicBarrier(2);
      var results = new java.util.ArrayList<Future<Boolean>>();
      for (String phone : new String[] { "0981112208", "0981112209" }) {
        results.add(
          threads.submit(() -> {
            ready.await(5, TimeUnit.SECONDS);
            try {
              registrations.start(phone, CAPTCHA);
              return true;
            } catch (IllegalArgumentException e) {
              assertThat(e.getMessage()).contains("10 lượt SMS");
              return false;
            }
          })
        );
      }
      assertThat(
        java.util.List.of(
          results.get(0).get(10, TimeUnit.SECONDS),
          results.get(1).get(10, TimeUnit.SECONDS)
        )
      ).containsExactlyInAnyOrder(true, false);
      verify(sms, times(10)).send(anyString(), eq(CAPTCHA));
      // Lượt cũ ra khỏi cửa sổ 24 giờ, kể cả sau khi tải lại ứng dụng.
      jdbc.update(
        "UPDATE registration_sms SET requested_at=now()-interval '24 hours 1 second' WHERE phone='0981112201'"
      );
      registrations.start("0981112201", CAPTCHA);
      verify(sms, times(11)).send(anyString(), eq(CAPTCHA));
    } finally {
      threads.shutdownNow();
      jdbc.update("DELETE FROM registration_sms WHERE phone LIKE '098111220_'");
      jdbc.update("DELETE FROM app_user WHERE username='phone-reviewer'");
    }
  }
}
