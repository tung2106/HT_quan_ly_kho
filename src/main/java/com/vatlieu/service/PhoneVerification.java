package com.vatlieu.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
@Slf4j
public class PhoneVerification {

  private static final List<String> SMS_ERROR_CODES = List.of(
    "BILLING_NOT_ENABLED",
    "CAPTCHA_CHECK_FAILED",
    "INVALID_APP_CREDENTIAL",
    "OPERATION_NOT_ALLOWED",
    "SMS_REGION_RESTRICTED",
    "SMS_REGION_BLOCKED",
    "QUOTA_EXCEEDED",
    "TOO_MANY_ATTEMPTS_TRY_LATER",
    "INVALID_PHONE_NUMBER",
    "API_KEY_SERVICE_BLOCKED",
    "API_KEY_IP_ADDRESS_BLOCKED",
    "API_KEY_HTTP_REFERRER_BLOCKED",
    "API_KEY_INVALID"
  );

  private final RestClient client;
  private final boolean configured;
  private volatile String siteKey;

  @Autowired
  public PhoneVerification(@Value("${FIREBASE_AUTH_API_KEY:}") String apiKey) {
    this(apiKey, httpClient());
  }

  private static RestClient.Builder httpClient() {
    var factory = new JdkClientHttpRequestFactory(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    );
    factory.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(factory);
  }

  PhoneVerification(String apiKey, RestClient.Builder builder) {
    configured = !apiKey.isBlank();
    client = builder
      .baseUrl("https://identitytoolkit.googleapis.com")
      .defaultHeader("X-Goog-Api-Key", apiKey)
      .build();
  }

  public boolean configured() {
    return configured;
  }

  public String captchaSiteKey() {
    if (!configured) return null;
    if (siteKey == null) {
      try {
        siteKey = requiredText(
          client.get().uri("/v1/recaptchaParams").retrieve().body(JsonNode.class),
          "recaptchaSiteKey"
        );
      } catch (RestClientException e) {
        throw unavailable();
      }
    }
    return siteKey;
  }

  public String send(String phone, String captcha) {
    try {
      var result = post(
        "accounts:sendVerificationCode",
        Map.of("phoneNumber", "+84" + phone.substring(1), "recaptchaToken", captcha)
      );
      return requiredText(result, "sessionInfo");
    } catch (RestClientResponseException e) {
      // Chỉ ghi lỗi đã biết, không ghi số điện thoại, mã hoặc nội dung phản hồi.
      String response = e.getResponseBodyAsString();
      String reason = SMS_ERROR_CODES.stream()
        .filter(response::contains)
        .findFirst()
        .orElse("UNKNOWN");
      log.warn("Gửi mã SMS thất bại. HTTP {}, mã lỗi {}", e.getStatusCode().value(), reason);
      if ("BILLING_NOT_ENABLED".equals(reason)) {
        throw new IllegalArgumentException(
          "Cửa hàng chưa thể gửi mã SMS. Vui lòng liên hệ quản lý."
        );
      }
      if (e instanceof HttpClientErrorException.BadRequest) {
        throw new IllegalArgumentException(
          "Chưa gửi được mã SMS. Hãy kiểm tra số điện thoại và xác nhận lại reCAPTCHA."
        );
      }
      throw unavailable();
    } catch (RestClientException e) {
      log.warn("Không nhận được phản hồi khi gửi mã SMS.");
      throw unavailable();
    }
  }

  public boolean approved(String sessionInfo, String phone, String code) {
    try {
      var result = post(
        "accounts:signInWithPhoneNumber",
        Map.of("sessionInfo", sessionInfo, "code", code)
      );
      // Chỉ tin kết quả máy chủ nhận trực tiếp từ Firebase cho phiên SMS đã lưu.
      return (
        result != null &&
        !result.path("localId").asText("").isBlank() &&
        !result.path("idToken").asText("").isBlank() &&
        ("+84" + phone.substring(1)).equals(result.path("phoneNumber").asText())
      );
    } catch (HttpClientErrorException.BadRequest e) {
      return false;
    } catch (RestClientException e) {
      throw unavailable();
    }
  }

  private JsonNode post(String path, Map<String, String> body) {
    if (!configured) throw unavailable();
    return client
      .post()
      .uri("/v1/" + path)
      .contentType(MediaType.APPLICATION_JSON)
      .body(body)
      .retrieve()
      .body(JsonNode.class);
  }

  private static String requiredText(JsonNode result, String key) {
    if (
      result == null || !result.path(key).isTextual() || result.path(key).asText().isBlank()
    ) throw unavailable();
    return result.path(key).asText();
  }

  private static IllegalArgumentException unavailable() {
    return new IllegalArgumentException(
      "Dịch vụ xác thực điện thoại tạm thời chưa sẵn sàng. Vui lòng thử lại sau."
    );
  }
}
