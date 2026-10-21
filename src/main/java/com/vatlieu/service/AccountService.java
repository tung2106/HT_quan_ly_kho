package com.vatlieu.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @RequiredArgsConstructor
public class AccountService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final CurrentUser current;
    public static class Principal extends User {
        public final int version;
        public Principal(String username,String password,boolean active,String role,int version) {
            super(username,password,active,true,true,true,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
            this.version=version;
        }
    }
    @Override public UserDetails loadUserByUsername(String username) {
        var rows=jdbc.queryForList("SELECT * FROM app_user WHERE username=?",username.trim().toLowerCase(Locale.ROOT));
        if(rows.isEmpty()) throw new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ");
        var row=rows.get(0);
        return new Principal((String)row.get("username"),(String)row.get("password"),(Boolean)row.get("active"),(String)row.get("role"),(Integer)row.get("auth_version"));
    }
    public static void validatePassword(String password) {
        if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte UTF-8");
    }
    private String validateName(String username) {
        String name=username==null?"":username.trim().toLowerCase(Locale.ROOT);
        if(!name.matches("[a-z0-9][a-z0-9._-]{2,49}")) throw new IllegalArgumentException("Tên đăng nhập: 3–50 chữ thường, số, dấu chấm, gạch ngang hoặc gạch dưới");
        return name;
    }
    public List<Map<String,Object>> list() {
        current.requireOwner();
        return jdbc.queryForList("SELECT id,username,full_name,role,active FROM app_user ORDER BY id");
    }
    @Transactional public void create(String username,String fullName,String role,String password) {
        current.requireOwner();
        insert(username,fullName,role,password);
    }
    private void insert(String username,String fullName,String role,String password) {
        String name=validateName(username);validatePassword(password);
        if(fullName==null || fullName.isBlank() || fullName.length()>100) throw new IllegalArgumentException("Họ tên cần từ 1 đến 100 ký tự");
        if(!Set.of("OWNER","STAFF").contains(role)) throw new IllegalArgumentException("Vai trò không hợp lệ");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE lower(username)=?",Integer.class,name)>0)
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        jdbc.update("INSERT INTO app_user(username,full_name,password,role) VALUES (?,?,?,?)",name,fullName.trim(),encoder.encode(password),role);
    }
    @Transactional public void bootstrap(String username,String password) {
        // Khóa riêng cho lần khởi tạo; không tạo thêm quản lý khi đã có tài khoản.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(834701)");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user",Integer.class)==0) insert(username,"Quản lý","OWNER",password);
    }
    @Transactional public void setActive(long id,boolean active) {
        current.requireOwner();
        if(id==current.id()) throw new IllegalArgumentException("Không được tự khóa tài khoản đang dùng");
        var rows=jdbc.queryForList("SELECT id,role,active FROM app_user ORDER BY id FOR UPDATE");
        var target=rows.stream().filter(r -> ((Number)r.get("id")).longValue()==id).findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        if(!active && "OWNER".equals(target.get("role")) && rows.stream().filter(r -> "OWNER".equals(r.get("role")) && Boolean.TRUE.equals(r.get("active"))).count()<=1)
            throw new IllegalArgumentException("Phải còn ít nhất một quản lý hoạt động");
        jdbc.update("UPDATE app_user SET active=?,auth_version=auth_version+1 WHERE id=?",active,id);
    }
    @Transactional public void resetPassword(long id,String password) {
        current.requireOwner();validatePassword(password);
        if(id==current.id()) throw new IllegalArgumentException("Hãy dùng mục Đổi mật khẩu của tôi");
        if(jdbc.update("UPDATE app_user SET password=?,must_change_password=true,auth_version=auth_version+1 WHERE id=?",encoder.encode(password),id)==0)
            throw new IllegalArgumentException("Không tìm thấy tài khoản");
    }
    @Transactional public void changePassword(String oldPassword,String newPassword,String confirm) {
        long id=current.id();validatePassword(newPassword);
        if(!newPassword.equals(confirm)) throw new IllegalArgumentException("Hai lần nhập mật khẩu mới chưa khớp");
        String old=jdbc.queryForObject("SELECT password FROM app_user WHERE id=? FOR UPDATE",String.class,id);
        if(!encoder.matches(oldPassword,old)) throw new IllegalArgumentException("Mật khẩu hiện tại chưa đúng");
        if(encoder.matches(newPassword,old)) throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        jdbc.update("UPDATE app_user SET password=?,must_change_password=false,auth_version=auth_version+1 WHERE id=?",encoder.encode(newPassword),id);
    }
}
