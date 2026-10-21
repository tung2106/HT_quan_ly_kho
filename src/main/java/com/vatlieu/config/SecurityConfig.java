package com.vatlieu.config;

import com.vatlieu.service.AccountService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http,JdbcTemplate jdbc) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login","/css/**","/js/**","/images/**","/error","/error/**").permitAll()
                .requestMatchers("/account/password","/logout").authenticated()
                .requestMatchers(HttpMethod.GET,"/","/stock","/products","/outbound","/outbound/**").authenticated()
                .requestMatchers(HttpMethod.POST,"/outbound").authenticated()
                .anyRequest().hasRole("OWNER"))
            .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/",true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .exceptionHandling(errors -> errors.accessDeniedPage("/error/403"));
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth!=null && auth.getPrincipal() instanceof AccountService.Principal principal) {
                    var rows=jdbc.queryForList("SELECT active,auth_version,must_change_password FROM app_user WHERE username=?",principal.getUsername());
                    if(rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).get("active")) || principal.version!=((Number)rows.get(0).get("auth_version")).intValue()) {
                        var session=request.getSession(false);if(session!=null) session.invalidate();
                        SecurityContextHolder.clearContext();response.sendRedirect(request.getContextPath()+"/login?expired");return;
                    }
                    String path=request.getRequestURI().substring(request.getContextPath().length());
                    if(Boolean.TRUE.equals(rows.get(0).get("must_change_password")) && !path.equals("/account/password") && !path.equals("/logout") && !path.startsWith("/css/") && !path.startsWith("/js/") && !path.startsWith("/images/") && !path.startsWith("/error")) {
                        response.sendRedirect(request.getContextPath()+"/account/password");return;
                    }
                }
                chain.doFilter(request,response);
            }
        },AuthorizationFilter.class);
        return http.build();
    }
    @Bean ApplicationRunner firstManager(AccountService accounts,Environment env) {
        return args -> {
            String username=env.getProperty("APP_ADMIN_USERNAME");
            String password=env.getProperty("APP_ADMIN_PASSWORD");
            if(username!=null && password!=null) accounts.bootstrap(username,password);
        };
    }
}
