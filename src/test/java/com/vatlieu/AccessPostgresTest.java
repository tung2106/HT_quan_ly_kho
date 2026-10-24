package com.vatlieu;

import com.vatlieu.service.AccountService;
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
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS",matches="true")
class AccessPostgresTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AccountService accounts;
    private final String password="Only-for-tests-123!";
    @BeforeEach void users() {
        for(String name:new String[]{"acl-owner","acl-a","acl-b"})
            jdbc.update("INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES (?,?,?,?,false)",name,name,encoder.encode(password),name.equals("acl-owner")?"OWNER":"STAFF");
    }
    private long id(String name) { return jdbc.queryForObject("SELECT id FROM app_user WHERE username=?",Long.class,name); }
    private long order(String name,String note,String kind) {
        return jdbc.queryForObject("INSERT INTO outbound_order(kind,customer,note,total_amount,cost_amount,request_key,created_by) VALUES (?,'Khách',?,100,73,?,?) RETURNING id",Long.class,kind,note,UUID.randomUUID(),name==null?null:id(name));
    }
    @Test void anonymousMustLoginAndNoRegistration() throws Exception {
        mvc.perform(get("/stock")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(post("/admin/users").with(csrf()).param("username","hacker")).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE username='hacker'",Integer.class)).isZero();
    }
    @Test @WithMockUser(username="acl-a",roles="STAFF")
    void staffCannotReadOtherOrdersOrCostsOrManagerRoutes() throws Exception {
        long mine=order("acl-a","MY-SALE","SALE");long other=order("acl-b","PRIVATE-OTHER-SALE","SALE");
        long legacy=order(null,"LEGACY-PRIVATE","SALE");long waste=order("acl-a","WASTE-PRIVATE","WASTE");
        mvc.perform(get("/outbound")).andExpect(status().isOk()).andExpect(content().string(not(containsString("PRIVATE-OTHER-SALE"))))
                .andExpect(content().string(not(containsString("LEGACY-PRIVATE"))));
        mvc.perform(get("/outbound/"+mine)).andExpect(status().isOk()).andExpect(content().string(containsString("MY-SALE")))
                .andExpect(content().string(not(containsString("Giá vốn")))).andExpect(content().string(not(containsString("Tổng giá vốn"))));
        for(long denied:new long[]{other,legacy,waste}) mvc.perform(get("/outbound/"+denied)).andExpect(status().isForbidden());
        for(String path:new String[]{"/capital","/purchases","/purchases/new","/categories","/suppliers","/products/new","/admin/users","/outbound/new?kind=WASTE"})
            mvc.perform(get(path)).andExpect(status().isForbidden());
        mvc.perform(post("/outbound").with(csrf()).param("kind","WASTE").param("note","Giả mạo")).andExpect(status().isForbidden());
        mvc.perform(get("/suppliers?deleted=true")).andExpect(status().isForbidden());
        for(String path:new String[]{"/products","/products/1/delete","/categories","/categories/1/edit","/categories/1/delete","/suppliers/1/delete","/suppliers/1/restore","/suppliers/1/edit","/capital/pay","/admin/users"})
            mvc.perform(post(path).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/stock")).andExpect(status().isOk()).andExpect(content().string(not(containsString("Giá vốn"))));
        mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(content().string(not(containsString("giá vốn"))));
    }
    @Test @WithMockUser(username="acl-a",roles="STAFF")
    void saleUsesAuthenticatedCreatorAndCsrf() throws Exception {
        long c=jdbc.queryForObject("INSERT INTO category(name) VALUES (?) RETURNING id",Long.class,"ACL "+UUID.randomUUID());
        long p=jdbc.queryForObject("INSERT INTO product(category_id,name,base_unit) VALUES (?,'ACL hàng','kg') RETURNING id",Long.class,c);
        long b=jdbc.queryForObject("INSERT INTO batch(product_id,qty_in,qty_left,unit_cost) VALUES (?,2,2,73) RETURNING id",Long.class,p);
        jdbc.update("INSERT INTO stock_transaction(product_id,batch_id,tx_type,qty_change,unit_cost) VALUES (?,?,'PURCHASE',2,73)",p,b);
        mvc.perform(post("/outbound").param("kind","SALE")).andExpect(status().isForbidden());
        String redirect=mvc.perform(post("/outbound").with(csrf()).param("kind","SALE")
                .param("createdBy",Long.toString(id("acl-b"))).param("items[0].productId",Long.toString(p))
                .param("items[0].qty","1").param("items[0].unit","kg").param("items[0].unitPrice","100")
                .param("paidAmount","100")).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        long sale=Long.parseLong(redirect.substring(redirect.lastIndexOf('/')+1));
        assertThat(jdbc.queryForObject("SELECT created_by FROM outbound_order WHERE id=?",Long.class,sale)).isEqualTo(id("acl-a"));
    }
    @Test @WithMockUser(username="acl-owner",roles="OWNER")
    void managerCategoryCrudAndProtection() throws Exception {
        String name="Danh mục "+UUID.randomUUID();
        mvc.perform(post("/categories").with(csrf()).param("name",name)).andExpect(redirectedUrl("/categories"));
        long c=jdbc.queryForObject("SELECT id FROM category WHERE name=?",Long.class,name);
        mvc.perform(post("/categories/"+c+"/edit").with(csrf()).param("name",name+" sửa").param("perishable","true")).andExpect(redirectedUrl("/categories"));
        assertThat(jdbc.queryForObject("SELECT perishable FROM category WHERE id=?",Boolean.class,c)).isTrue();
        jdbc.update("INSERT INTO product(category_id,name,base_unit) VALUES (?,'Hàng','kg')",c);
        mvc.perform(post("/categories/"+c+"/delete").with(csrf())).andExpect(flash().attribute("error",containsString("đang có sản phẩm")));
        long empty=jdbc.queryForObject("INSERT INTO category(name) VALUES (?) RETURNING id",Long.class,"Trống "+UUID.randomUUID());
        mvc.perform(post("/categories/"+empty+"/delete").with(csrf())).andExpect(flash().attribute("msg","Đã xóa danh mục"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id=?",Integer.class,empty)).isZero();
        mvc.perform(get("/categories")).andExpect(status().isOk()).andExpect(content().string(containsString("Sửa danh mục")));
        mvc.perform(get("/admin/users")).andExpect(status().isOk());
        mvc.perform(get("/purchases/new")).andExpect(status().isOk()).andExpect(content().string(not(containsString("items[1].productId"))));
    }
    @Test @WithMockUser(username="acl-owner",roles="OWNER")
    void passwordsHashedAndSelfLockDenied() {
        accounts.create("acl-new","Người mới","STAFF",password);
        String hash=jdbc.queryForObject("SELECT password FROM app_user WHERE username='acl-new'",String.class);
        assertThat(hash).isNotEqualTo(password);assertThat(encoder.matches(password,hash)).isTrue();
        assertThatThrownBy(() -> accounts.setActive(id("acl-owner"),false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accounts.create("acl-other","Tên","ROOT",password)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void realLoginAndSessionRevocation() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username","acl-a").param("password","wrong")).andExpect(redirectedUrl("/login?error"));
        MockHttpSession session=(MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username","acl-a").param("password",password))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk());
        jdbc.update("UPDATE app_user SET active=false,auth_version=auth_version+1 WHERE username='acl-a'");
        mvc.perform(get("/stock").session(session)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(post("/login").with(csrf()).param("username","acl-a").param("password",password)).andExpect(redirectedUrl("/login?error"));
    }
    @Test void firstLoginMustChangePassword() throws Exception {
        jdbc.update("UPDATE app_user SET must_change_password=true WHERE username='acl-a'");
        MockHttpSession session=(MockHttpSession)mvc.perform(post("/login").with(csrf()).param("username","acl-a").param("password",password)).andReturn().getRequest().getSession(false);
        mvc.perform(get("/stock").session(session)).andExpect(redirectedUrl("/account/password"));
        mvc.perform(get("/account/password").session(session)).andExpect(status().isOk());
        mvc.perform(post("/account/password").session(session).with(csrf()).param("oldPassword",password).param("newPassword","New-test-password-456!").param("confirm","New-test-password-456!"))
                .andExpect(redirectedUrl("/login?changed"));
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM app_user WHERE username='acl-a'",Boolean.class)).isFalse();
    }
}
