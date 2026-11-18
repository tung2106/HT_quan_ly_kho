package com.vatlieu.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PhoneVerificationTest {

  @Test
  void usesServerSideFirebaseSessionAndChecksTheVerifiedPhone() {
    String base = "https://identitytoolkit.googleapis.com/v1/";
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var sms = new PhoneVerification("server-key-for-test", builder);
    server
      .expect(requestTo(base + "recaptchaParams"))
      .andExpect(method(HttpMethod.GET))
      .andExpect(header("X-Goog-Api-Key", "server-key-for-test"))
      .andRespond(
        withSuccess("{\"recaptchaSiteKey\":\"public-site-key\"}", MediaType.APPLICATION_JSON)
      );
    server
      .expect(requestTo(base + "accounts:sendVerificationCode"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json("{\"phoneNumber\":\"+84981234567\",\"recaptchaToken\":\"captcha\"}", true)
      )
      .andRespond(
        withSuccess("{\"sessionInfo\":\"session-from-firebase\"}", MediaType.APPLICATION_JSON)
      );
    for (String number : new String[] { "+84981234567", "+84981234568" }) {
      server
        .expect(requestTo(base + "accounts:signInWithPhoneNumber"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
          content().json("{\"sessionInfo\":\"session-from-firebase\",\"code\":\"123456\"}", true)
        )
        .andRespond(
          withSuccess(
            "{\"localId\":\"user-id\",\"idToken\":\"test-id-token\",\"phoneNumber\":\"" +
              number +
              "\"}",
            MediaType.APPLICATION_JSON
          )
        );
    }
    server.expect(requestTo(base + "accounts:signInWithPhoneNumber")).andRespond(withBadRequest());
    server.expect(requestTo(base + "accounts:sendVerificationCode")).andRespond(withServerError());
    assertThat(sms.captchaSiteKey()).isEqualTo("public-site-key");
    assertThat(sms.send("0981234567", "captcha")).isEqualTo("session-from-firebase");
    assertThat(sms.approved("session-from-firebase", "0981234567", "123456")).isTrue();
    assertThat(sms.approved("session-from-firebase", "0981234567", "123456")).isFalse();
    assertThat(sms.approved("session-from-firebase", "0981234567", "000000")).isFalse();
    assertThatThrownBy(() -> sms.send("0981234567", "captcha")).isInstanceOf(
      IllegalArgumentException.class
    );
    server.verify();
  }
}
