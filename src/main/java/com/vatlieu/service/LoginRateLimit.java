package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginRateLimit {

  private final JdbcTemplate jdbc;

  @Transactional
  public int retryAfterSeconds(String username) {
    // Đặt chỗ trước khi so mật khẩu. Các request đồng thời và các máy chủ dùng cùng bộ đếm.
    if (!reserve("global", 120, 60)) return 60;
    jdbc.update(
      "DELETE FROM login_rate_limit WHERE expires_at <= statement_timestamp() AND bucket <> 'global'"
    );
    String account = MobileNumber.login(username);
    if (!account.matches("[a-z0-9][a-z0-9._-]{2,49}")) account = "invalid!";
    return reserve("user:" + account, 8, 600) ? 0 : 600;
  }

  private boolean reserve(String bucket, int maxAttempts, int seconds) {
    return (
      jdbc.update(
        """
        INSERT INTO login_rate_limit AS l (bucket, attempts, expires_at)
        VALUES (?, 1, statement_timestamp() + make_interval(secs => ?))
        ON CONFLICT (bucket) DO UPDATE SET
          attempts = CASE WHEN l.expires_at <= statement_timestamp() THEN 1 ELSE l.attempts + 1 END,
          expires_at = CASE WHEN l.expires_at <= statement_timestamp() THEN EXCLUDED.expires_at ELSE l.expires_at END
        WHERE l.expires_at <= statement_timestamp() OR l.attempts < ?
        """,
        bucket,
        seconds,
        maxAttempts
      ) == 1
    );
  }
}
