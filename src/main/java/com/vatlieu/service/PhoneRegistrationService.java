package com.vatlieu.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PhoneRegistrationService {

  public static final int CONSENT_VERSION = 1;

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final PasswordEncoder passwords;
  private final PhoneVerification sms;

  public record Challenge(UUID id, String phone) {}

  private record SmsAttempt(String sessionInfo, int consentVersion, OffsetDateTime consentedAt) {}

  public Challenge start(
    String input,
    String captcha,
    int consentVersion,
    boolean acceptedTerms,
    boolean acceptedPhoneProcessing
  ) {
    if (!acceptedTerms || !acceptedPhoneProcessing || consentVersion != CONSENT_VERSION) {
      throw new IllegalArgumentException(
        "Hãy đọc và đánh dấu cả hai ô đồng ý trước khi yêu cầu mã SMS."
      );
    }
    String phone = MobileNumber.require(input);
    if (!sms.configured()) throw new IllegalArgumentException(
      "Cửa hàng chưa bật gửi mã SMS. Hãy liên hệ quản lý."
    );
    if (
      captcha == null || captcha.isBlank() || captcha.length() > 8192
    ) throw new IllegalArgumentException("Hãy hoàn thành bước kiểm tra reCAPTCHA rồi yêu cầu mã.");
    var challenge = new Challenge(UUID.randomUUID(), phone);
    boolean send = Boolean.TRUE.equals(
      transactions.execute(status -> {
        // Khóa ngắn trong CSDL để nhiều lần bấm hoặc nhiều máy chủ cùng tuân thủ hạn mức.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(834702)");
        jdbc.update(
          "DELETE FROM registration_sms WHERE requested_at <= statement_timestamp() - interval '24 hours'"
        );
        if (
          !jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM app_user WHERE active AND role='OWNER')",
            Boolean.class
          )
        ) throw new IllegalArgumentException("Cửa hàng chưa mở đăng ký. Hãy liên hệ quản lý.");
        var usage = jdbc.queryForMap(
          """
          SELECT count(*) >= 10 AS full, count(*) FILTER (WHERE phone=?) >= 5
            OR count(*) FILTER (WHERE phone=? AND requested_at > statement_timestamp() - interval '60 seconds') > 0 AS phone_limited
          FROM registration_sms
          """,
          phone,
          phone
        );
        if (Boolean.TRUE.equals(usage.get("full"))) throw new IllegalArgumentException(
          "Cửa hàng đã dùng hết 10 lượt SMS trong 24 giờ. Vui lòng thử lại khi lượt cũ nhất đã đủ 24 giờ."
        );
        if (Boolean.TRUE.equals(usage.get("phone_limited"))) throw new IllegalArgumentException(
          "Mỗi số được yêu cầu tối đa 5 lần trong 24 giờ, cách nhau ít nhất 60 giây."
        );
        if (
          Boolean.TRUE.equals(
            jdbc.queryForObject(
              "SELECT EXISTS (SELECT 1 FROM app_user WHERE username=?)",
              Boolean.class,
              phone
            )
          )
        ) return false;
        jdbc.update(
          "INSERT INTO registration_sms(id,phone,requested_at,consent_version) VALUES (?,?,statement_timestamp(),?)",
          challenge.id(),
          phone,
          consentVersion
        );
        return true;
      })
    );
    if (send) {
      String sessionInfo = sms.send(phone, captcha);
      jdbc.update(
        "UPDATE registration_sms SET firebase_session_info=? WHERE id=?",
        sessionInfo,
        challenge.id()
      );
    }
    return challenge;
  }

  public void complete(Challenge challenge, String code, String fullName, String password) {
    if (challenge == null) throw new IllegalArgumentException(
      "Phiên đăng ký đã hết. Hãy yêu cầu mã mới."
    );
    if (
      fullName == null || fullName.isBlank() || fullName.trim().length() > 100
    ) throw new IllegalArgumentException("Họ tên cần từ 1 đến 100 ký tự.");
    AccountService.validatePassword(password);
    if (code == null || !code.matches("[0-9]{6}")) throw invalidCode();
    var attempts = jdbc.query(
      """
      UPDATE registration_sms SET attempts=attempts+1 WHERE id=? AND phone=? AND attempts < 5
        AND requested_at > statement_timestamp() - interval '10 minutes'
        AND firebase_session_info IS NOT NULL AND consent_version IS NOT NULL
      RETURNING firebase_session_info, consent_version, requested_at
      """,
      (row, index) ->
        new SmsAttempt(
          row.getString("firebase_session_info"),
          row.getInt("consent_version"),
          row.getObject("requested_at", OffsetDateTime.class)
        ),
      challenge.id(),
      challenge.phone()
    );
    if (attempts.isEmpty()) throw invalidCode();
    SmsAttempt attempt = attempts.get(0);
    if (!sms.approved(attempt.sessionInfo(), challenge.phone(), code)) throw invalidCode();
    String hash = passwords.encode(password);
    transactions.executeWithoutResult(status -> {
      int consumed = jdbc.update(
        "UPDATE registration_sms SET firebase_session_info=NULL WHERE id=? AND firebase_session_info=?",
        challenge.id(),
        attempt.sessionInfo()
      );
      if (consumed == 0) throw invalidCode();
      jdbc.update(
        """
        INSERT INTO app_user(username,full_name,password,role,active,pending_approval,must_change_password,
          registration_consent_version,registration_consented_at)
        VALUES (?,?,?,'STAFF',false,true,false,?,?) ON CONFLICT (username) DO NOTHING
        """,
        challenge.phone(),
        fullName.trim(),
        hash,
        attempt.consentVersion(),
        attempt.consentedAt()
      );
    });
  }

  private IllegalArgumentException invalidCode() {
    return new IllegalArgumentException(
      "Mã không đúng hoặc đã hết hiệu lực. Kiểm tra mã hoặc yêu cầu mã mới."
    );
  }
}
