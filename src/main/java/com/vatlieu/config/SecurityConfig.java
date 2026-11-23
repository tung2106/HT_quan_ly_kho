package com.vatlieu.config;

import com.vatlieu.service.AccountService;
import com.vatlieu.service.LoginRateLimit;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String PAGE_POLICY =
    "style-src 'self' https://cdn.jsdelivr.net; style-src-attr 'unsafe-inline'; " +
    "img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'";

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, JdbcTemplate jdbc, LoginRateLimit loginLimit)
    throws Exception {
    http
      .headers(headers ->
        headers
          .referrerPolicy(policy -> policy.policy(ReferrerPolicy.NO_REFERRER))
          .contentSecurityPolicy(csp ->
            csp.policyDirectives("default-src 'self'; script-src 'self'; " + PAGE_POLICY)
          )
      )
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error", "/error/**")
          .permitAll()
          .requestMatchers("/account/password", "/logout")
          .authenticated()
          .requestMatchers("/admin/**")
          .hasRole("OWNER")
          .requestMatchers(
            "/",
            "/stock/**",
            "/products/**",
            "/outbound/**",
            "/purchases/**",
            "/capital/**",
            "/categories/**",
            "/suppliers/**"
          )
          .hasAnyRole("OWNER", "STAFF")
          .anyRequest()
          .denyAll()
      )
      .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
      .logout(logout ->
        logout
          .logoutSuccessUrl("/login?logout")
          .invalidateHttpSession(true)
          .deleteCookies("JSESSIONID")
      )
      .exceptionHandling(errors -> errors.accessDeniedPage("/error/403"));
    var loginPost = new AntPathRequestMatcher("/login", "POST");
    http.addFilterBefore(
      new OncePerRequestFilter() {
        @Override
        protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain chain
        ) throws ServletException, IOException {
          if (loginPost.matches(request)) {
            int retry = loginLimit.retryAfterSeconds(request.getParameter("username"));
            if (retry > 0) {
              response.setStatus(429);
              response.setHeader("Retry-After", Integer.toString(retry));
              response.setContentType("text/plain;charset=UTF-8");
              response
                .getWriter()
                .write("Đã đạt giới hạn đăng nhập. Vui lòng thử lại sau " + retry / 60 + " phút.");
              return;
            }
          }
          chain.doFilter(request, response);
        }
      },
      UsernamePasswordAuthenticationFilter.class
    );
    http.addFilterBefore(
      new OncePerRequestFilter() {
        @Override
        protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain chain
        ) throws ServletException, IOException {
          var auth = SecurityContextHolder.getContext().getAuthentication();
          if (auth != null && auth.getPrincipal() instanceof AccountService.Principal principal) {
            var rows = jdbc.queryForList(
              "SELECT active,role,auth_version,must_change_password FROM app_user WHERE username=? AND deleted_at IS NULL",
              principal.getUsername()
            );
            if (
              rows.isEmpty() ||
              !Boolean.TRUE.equals(rows.get(0).get("active")) ||
              principal
                .getAuthorities()
                .stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_" + rows.get(0).get("role"))) ||
              principal.version != ((Number) rows.get(0).get("auth_version")).intValue()
            ) {
              var session = request.getSession(false);
              if (session != null) session.invalidate();
              SecurityContextHolder.clearContext();
              response.sendRedirect(request.getContextPath() + "/login?expired");
              return;
            }
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (
              Boolean.TRUE.equals(rows.get(0).get("must_change_password")) &&
              !path.equals("/account/password") &&
              !path.equals("/logout") &&
              !path.startsWith("/css/") &&
              !path.startsWith("/js/") &&
              !path.startsWith("/images/") &&
              !path.startsWith("/error")
            ) {
              response.sendRedirect(request.getContextPath() + "/account/password");
              return;
            }
          }
          chain.doFilter(request, response);
        }
      },
      AuthorizationFilter.class
    );
    return http.build();
  }

  @Bean
  ApplicationRunner firstManager(AccountService accounts, Environment env) {
    return args -> {
      String username = env.getProperty("APP_ADMIN_USERNAME");
      String password = env.getProperty("APP_ADMIN_PASSWORD");
      if (username != null && password != null) accounts.bootstrap(username, password);
    };
  }
}
