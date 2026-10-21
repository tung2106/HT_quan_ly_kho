package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class CurrentUser {
    private final JdbcTemplate jdbc;
    public String username() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Cần đăng nhập");
        return auth.getName();
    }
    public long id() {
        var ids=jdbc.queryForList("SELECT id FROM app_user WHERE username=? AND active",Long.class,username());
        if(ids.isEmpty()) throw new AccessDeniedException("Tài khoản không hoạt động");
        return ids.get(0);
    }
    public boolean owner() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_user WHERE username=? AND active AND role='OWNER')",Boolean.class,username()));
    }
    public void requireOwner() {
        if(!owner()) throw new AccessDeniedException("Chức năng dành cho quản lý");
    }
}
