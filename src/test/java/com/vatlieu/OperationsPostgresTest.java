package com.vatlieu;

import com.vatlieu.form.*;
import com.vatlieu.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS",matches="true")
@org.springframework.security.test.context.support.WithMockUser(username="suite-owner",roles="OWNER")
@SpringBootTest @AutoConfigureMockMvc @Transactional
class OperationsPostgresTest {
    @Autowired OperationsService service;
    @Autowired PurchaseService purchases;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired javax.sql.DataSource dataSource;
    @org.junit.jupiter.api.BeforeEach void actor() { jdbc.update("INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES ('suite-owner','Test','unused','OWNER',false) ON CONFLICT DO NOTHING"); }
    @org.junit.jupiter.api.AfterEach void removeActor() { jdbc.update("DELETE FROM app_user WHERE username='suite-owner' AND id NOT IN (SELECT created_by FROM outbound_order WHERE created_by IS NOT NULL)"); }
    private BigDecimal n(String value) { return new BigDecimal(value); }
    private long product() {
        long category = jdbc.queryForObject("INSERT INTO category(name) VALUES (?) RETURNING id",Long.class,"Kiểm thử " + UUID.randomUUID());
        long p = jdbc.queryForObject("INSERT INTO product(category_id,name,base_unit) VALUES (?,'Hàng kiểm thử','kg') RETURNING id",Long.class,category);
        jdbc.update("INSERT INTO unit_conversion(product_id,unit_name,factor) VALUES (?,'bao',50)",p);
        return p;
    }
    private PurchaseItemForm row(long p,String qty,String unit,String price) {
        var row = new PurchaseItemForm(); row.setProductId(p);row.setQty(n(qty));row.setUnit(unit);row.setUnitPrice(n(price));return row;
    }
    private long receive(long p,String qty,String price,String paid) {
        var f = new PurchaseForm();f.getItems().add(row(p,qty,"kg",price));f.setPaidAmount(n(paid));
        long id = purchases.create(f).getId(); em.flush();em.clear();return id;
    }
    private OutboundForm sale(long p,String qty,String price) {
        var f = new OutboundForm();f.setCustomer("Khách kiểm thử");f.getItems().add(row(p,qty,"kg",price));return f;
    }
    private void decimal(String sql,long id,String expected) {
        assertThat(jdbc.queryForObject(sql,BigDecimal.class,id)).isEqualByComparingTo(expected);
    }
    @Test void xuatBanHaoHutCongNoVaRenderGiaoDien() throws Exception {
        var before=service.summary();
        long p=product(); long purchase=receive(p,"2","100","50");receive(p,"3","200","0");
        var f=sale(p,"3","300");f.setPaidAmount(n("100"));long id=service.create(f);
        assertThat(service.create(f)).isEqualTo(id);
        decimal("SELECT cost_amount FROM outbound_order WHERE id=?",id,"400");
        decimal("SELECT avg_cost FROM product WHERE id=?",p,"200");
        decimal("SELECT SUM(qty_change) FROM stock_transaction WHERE product_id=?",p,"2");
        UUID payment=UUID.randomUUID(); service.pay("sale",id,n("400"),payment,"Thu lần 2");
        service.pay("sale",id,n("400"),payment,"Gửi lại");
        service.pay("sale",id,n("400"),UUID.randomUUID(),"Thu hết");
        service.pay("purchase",purchase,n("150"),UUID.randomUUID(),"Trả hết");
        decimal("SELECT paid_amount FROM outbound_order WHERE id=?",id,"900");
        decimal("SELECT SUM(amount) FROM cash_payment WHERE purchase_id=?",purchase,"200");
        var waste=sale(p,"0.5","0");waste.setKind("WASTE");waste.setNote("Hỏng do ẩm");
        long wasteId=service.create(waste);
        decimal("SELECT cost_amount FROM outbound_order WHERE id=?",wasteId,"100");
        for(String url : new String[]{"/outbound","/outbound/new","/outbound/new?kind=WASTE","/outbound/"+id,"/outbound/"+wasteId,"/capital","/stock","/purchases"})
            mvc.perform(get(url)).andExpect(status().isOk());
        org.springframework.security.core.context.SecurityContextHolder.setContext(org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        service.create(sale(p,"1.5","300"));
        decimal("SELECT SUM(qty_change) FROM stock_transaction WHERE product_id=?",p,"0");
        decimal("SELECT avg_cost FROM product WHERE id=?",p,"0");
        var after=service.summary();
        assertThat(((BigDecimal)after.get("revenue")).subtract((BigDecimal)before.get("revenue"))).isEqualByComparingTo("1350");
        assertThat(((BigDecimal)after.get("sold_cost")).subtract((BigDecimal)before.get("sold_cost"))).isEqualByComparingTo("700");
        assertThat(((BigDecimal)after.get("waste")).subtract((BigDecimal)before.get("waste"))).isEqualByComparingTo("100");
        assertThat(((BigDecimal)after.get("receipts")).subtract((BigDecimal)before.get("receipts"))).isEqualByComparingTo("900");
        assertThat(((BigDecimal)after.get("payments")).subtract((BigDecimal)before.get("payments"))).isEqualByComparingTo("200");
        assertThat(((BigDecimal)after.get("receivable")).subtract((BigDecimal)before.get("receivable"))).isEqualByComparingTo("450");
        assertThat(((BigDecimal)after.get("payable")).subtract((BigDecimal)before.get("payable"))).isEqualByComparingTo("600");
    }
    @Test void quyDoiVaLamTronTongThayViTungDong() {
        long p=product();receive(p,"100","0.1","0");
        var f=sale(p,"0.004","100");f.getItems().get(0).setUnit("bao");
        f.getItems().add(row(p,"0.004","bao","100"));
        long id=service.create(f);
        decimal("SELECT total_amount FROM outbound_order WHERE id=?",id,"1");
        decimal("SELECT SUM(qty_change) FROM stock_transaction WHERE product_id=?",p,"99.6");
    }
    @Test void tuChoiBanVuotTon() {
        long p=product();receive(p,"1","10","0");
        assertThatThrownBy(() -> service.create(sale(p,"1.001","20"))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Không đủ");
    }
    @Test void tuChoiThuQuaNo() {
        long p=product();receive(p,"1","10","0");long id=service.create(sale(p,"1","20"));
        assertThatThrownBy(() -> service.pay("sale",id,n("21"),UUID.randomUUID(),"")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void tuChoiDonViChuaKhaiBao() {
        long p=product();var f=sale(p,"1","20");f.getItems().get(0).setUnit("thung");
        assertThatThrownBy(() -> service.create(f)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Chưa khai báo");
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void nangCapPhieuNhapCuKhongMatSoDaTra() {
        String schema="migration_test_"+UUID.randomUUID().toString().replace("-","");
        try {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("1").load().migrate();
            long c=jdbc.queryForObject("INSERT INTO "+schema+".category(name) VALUES ('Cũ') RETURNING id",Long.class);
            long p=jdbc.queryForObject("INSERT INTO "+schema+".product(category_id,name,base_unit) VALUES (?,'Cũ','kg') RETURNING id",Long.class,c);
            long o=jdbc.queryForObject("INSERT INTO "+schema+".purchase_order(shipping_cost,paid_amount) VALUES (0.2,10) RETURNING id",Long.class);
            jdbc.update("INSERT INTO "+schema+".purchase_item(order_id,product_id,qty,unit_cost) VALUES (?,?,0.104,100)",o,p);
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
            decimal("SELECT total_amount FROM "+schema+".purchase_order WHERE id=?",o,"11");
            decimal("SELECT paid_amount FROM "+schema+".purchase_order WHERE id=?",o,"10");
            decimal("SELECT amount FROM "+schema+".cash_payment WHERE purchase_id=?",o,"10");
        } finally {
            // Tên schema do kiểm thử sinh, không chạm vào schema ứng dụng.
            jdbc.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
        }
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void haiNguoiCungBanKhongAmKhoVaRollbackPhieuLoi() throws Exception {
        long p=product();
        long category=jdbc.queryForObject("SELECT category_id FROM product WHERE id=?",Long.class,p);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var input=new PurchaseForm();input.getItems().add(row(p,"1","kg","10"));
            long purchase=purchases.create(input).getId();
            // Dòng đầu hợp lệ, dòng thứ hai vượt tồn: toàn phiếu phải rollback.
            var bad=sale(p,"0.5","20");bad.getItems().add(row(p,"1","kg","20"));
            assertThatThrownBy(() -> service.create(bad)).isInstanceOf(IllegalArgumentException.class);
            decimal("SELECT SUM(qty_change) FROM stock_transaction WHERE product_id=?",p,"1");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM outbound_order WHERE request_key=?",Integer.class,bad.getRequestKey())).isZero();
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> sell=() -> {
                start.await();
                try { service.create(sale(p,"1","20"));return true; }
                catch(IllegalArgumentException e) { return false; }
            };
            var a=pool.submit(new org.springframework.security.concurrent.DelegatingSecurityContextCallable<>(sell));var b=pool.submit(new org.springframework.security.concurrent.DelegatingSecurityContextCallable<>(sell));start.countDown();
            assertThat(java.util.List.of(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            decimal("SELECT SUM(qty_change) FROM stock_transaction WHERE product_id=?",p,"0");
            long saleId=jdbc.queryForObject("SELECT order_id FROM outbound_item WHERE product_id=?",Long.class,p);
            var payStart=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> pay=() -> {
                payStart.await();
                try { service.pay("sale",saleId,n("15"),UUID.randomUUID(),"");return true; }
                catch(IllegalArgumentException e) { return false; }
            };
            var c=pool.submit(new org.springframework.security.concurrent.DelegatingSecurityContextCallable<>(pay));var d=pool.submit(new org.springframework.security.concurrent.DelegatingSecurityContextCallable<>(pay));payStart.countDown();
            assertThat(java.util.List.of(c.get(15,java.util.concurrent.TimeUnit.SECONDS),d.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            decimal("SELECT paid_amount FROM outbound_order WHERE id=?",saleId,"15");
        } finally {
            pool.shutdownNow();pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);
            // Chỉ dọn các dòng thuộc sản phẩm kiểm thử vừa tạo.
            var orders=jdbc.queryForList("SELECT DISTINCT order_id FROM outbound_item WHERE product_id=?",Long.class,p);
            for(long id:orders) {
                jdbc.update("DELETE FROM cash_payment WHERE outbound_id=?",id);
                jdbc.update("DELETE FROM outbound_item WHERE order_id=?",id);
                jdbc.update("DELETE FROM outbound_order WHERE id=?",id);
            }
            var purchases=jdbc.queryForList("SELECT DISTINCT order_id FROM purchase_item WHERE product_id=?",Long.class,p);
            jdbc.update("DELETE FROM stock_transaction WHERE product_id=?",p);
            jdbc.update("DELETE FROM batch WHERE product_id=?",p);
            for(long id:purchases) {
                jdbc.update("DELETE FROM cash_payment WHERE purchase_id=?",id);
                jdbc.update("DELETE FROM purchase_item WHERE order_id=?",id);
                jdbc.update("DELETE FROM purchase_order WHERE id=?",id);
            }
            jdbc.update("DELETE FROM unit_conversion WHERE product_id=?",p);
            jdbc.update("DELETE FROM product WHERE id=?",p);
            jdbc.update("DELETE FROM category WHERE id=?",category);
        }
    }
}
