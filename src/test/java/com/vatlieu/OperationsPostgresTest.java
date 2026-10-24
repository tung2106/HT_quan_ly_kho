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
    @Autowired SupplierService suppliers;
    @Autowired ProductService productService;
    @Autowired StockService stockService;

    @Test void tatToanPhieuMoiVaCongNoKhongGhiTrung() throws Exception {
        long p=product();
        var purchaseForm=new PurchaseForm();purchaseForm.getItems().add(row(p,"1.5","kg","101"));
        purchaseForm.setShippingCost(n("10"));purchaseForm.setPaidAmount(n("999"));purchaseForm.setSettleAll(true);
        long purchase=purchases.create(purchaseForm).getId();em.flush();em.clear();
        decimal("SELECT paid_amount FROM purchase_order WHERE id=?",purchase,"162");
        decimal("SELECT SUM(amount) FROM cash_payment WHERE purchase_id=?",purchase,"162");
        var saleForm=sale(p,"1.5","101");saleForm.setSettleAll(true);saleForm.setCustomer(null);
        long fullSale=service.create(saleForm);
        decimal("SELECT paid_amount FROM outbound_order WHERE id=?",fullSale,"152");
        decimal("SELECT SUM(amount) FROM cash_payment WHERE outbound_id=?",fullSale,"152");
        assertThat(service.create(saleForm)).isEqualTo(fullSale);
        long debtPurchase=receive(p,"10","100","200");
        service.pay("purchase",debtPurchase,n("100"),UUID.randomUUID(),"Trả một phần");
        UUID key=UUID.randomUUID();
        service.pay("purchase",debtPurchase,null,key,"",true);
        service.pay("purchase",debtPurchase,null,key,"",true);
        service.pay("purchase",debtPurchase,null,UUID.randomUUID(),"",true);
        decimal("SELECT SUM(amount) FROM cash_payment WHERE purchase_id=?",debtPurchase,"1000");
        var partial=sale(p,"1","200");partial.setPaidAmount(n("50"));long debtSale=service.create(partial);
        service.pay("sale",debtSale,n("50"),UUID.randomUUID(),"Thu thêm");
        mvc.perform(post("/capital/pay").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("type","sale").param("id",Long.toString(debtSale)).param("requestKey",UUID.randomUUID().toString()).param("settleAll","true"))
                .andExpect(redirectedUrl("/capital?section=debts"));
        decimal("SELECT paid_amount FROM outbound_order WHERE id=?",debtSale,"200");
        decimal("SELECT SUM(amount) FROM cash_payment WHERE outbound_id=?",debtSale,"200");
    }

    @Test void chonLoBanTinhDungVonVaChanVuotTon() throws Exception {
        long p=product();receive(p,"100","4","0");receive(p,"50.5","6","0");
        var lots=jdbc.queryForList("SELECT id FROM batch WHERE product_id=? ORDER BY id",Long.class,p);
        var f=sale(p,"1","10");f.getItems().get(0).setUnit("bao");f.getItems().get(0).setBatchId(lots.get(1));
        long id=service.create(f);
        decimal("SELECT cost_amount FROM outbound_order WHERE id=?",id,"300");
        decimal("SELECT qty_left FROM batch WHERE id=?",lots.get(0),"100");
        decimal("SELECT qty_left FROM batch WHERE id=?",lots.get(1),"0.5");
        var excess=sale(p,"0.501","10");excess.getItems().get(0).setBatchId(lots.get(1));
        assertThatThrownBy(() -> service.create(excess)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vượt tồn");
        var exact=sale(p,"0.5","10");exact.getItems().get(0).setBatchId(lots.get(1));
        decimal("SELECT cost_amount FROM outbound_order WHERE id=?",service.create(exact),"3");
        decimal("SELECT avg_cost FROM product WHERE id=?",p,"4");
        excess.setRequestKey(UUID.randomUUID());
        assertThatThrownBy(() -> service.create(excess)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Lô đã hết");
        long other=product();receive(other,"1","99","0");
        long wrong=jdbc.queryForObject("SELECT id FROM batch WHERE product_id=?",Long.class,other);
        var invalid=sale(p,"1","10");invalid.getItems().get(0).setBatchId(wrong);
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không thuộc");
        assertThat(stockService.lots(false)).allSatisfy(l -> assertThat(l).doesNotContainKey("unit_cost"));
        mvc.perform(get("/stock/lots")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Các lô còn tồn")));
        mvc.perform(get("/outbound/new")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Lô xuất bán")));
        mvc.perform(get("/stock/lots").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff").roles("STAFF"))).andExpect(status().isForbidden());
    }

    @Test void xoaSanPhamGiuTonVaVon() throws Exception {
        long p=product();receive(p,"2","100","0");
        var before=service.summary();
        mvc.perform(get("/products")).andExpect(content().string(org.hamcrest.Matchers.containsString("/products/"+p+"/delete")));
        mvc.perform(post("/products/"+p+"/delete")).andExpect(status().isForbidden());
        mvc.perform(post("/products/"+p+"/delete").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/products"));
        em.flush();em.clear();
        org.springframework.security.core.context.SecurityContextHolder.setContext(org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        assertThat(jdbc.queryForObject("SELECT active FROM product WHERE id=?",Boolean.class,p)).isFalse();
        assertThat(stockService.findAll()).anySatisfy(r -> {
            assertThat(r.productId()).isEqualTo(p);assertThat(r.active()).isFalse();
            assertThat(r.qtyOnHand()).isEqualByComparingTo("2");assertThat(r.value()).isEqualByComparingTo("200");
        });
        assertThat(service.summary().get("inventory")).isEqualTo(before.get("inventory"));
        assertThatThrownBy(() -> service.create(sale(p,"1","150"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> receive(p,"1","100","0")).isInstanceOf(IllegalArgumentException.class);
        productService.delete(p);
        mvc.perform(get("/stock")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Ngừng kinh doanh")));
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired javax.sql.DataSource dataSource;
    @org.junit.jupiter.api.BeforeEach void actor() { jdbc.update("INSERT INTO app_user(username,full_name,password,role,must_change_password) VALUES ('suite-owner','Test','unused','OWNER',false) ON CONFLICT DO NOTHING"); }
    @org.junit.jupiter.api.AfterEach void removeActor() { jdbc.update("DELETE FROM app_user WHERE username='suite-owner' AND id NOT IN (SELECT created_by FROM outbound_order WHERE created_by IS NOT NULL) AND id NOT IN (SELECT created_by FROM cash_payment WHERE created_by IS NOT NULL)"); }
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
    @Test void nongSanCaLoTungBaoVaFifoKhongCanDanhMuc() throws Exception {
        var productForm=new ProductForm();productForm.setName("Nông sản thử "+UUID.randomUUID());
        productForm.setProductGroup("AGRICULTURE");productForm.setBaseUnit("bao");
        var product=productService.save(productForm); em.flush();long p=product.getId();
        assertThat(product.getCategory()).isNull();assertThat(product.getBaseUnit()).isEqualTo("kg");
        var f=new PurchaseForm();var lot=row(p,"3","kg","0");lot.setEntryMode("LOT");lot.setLotAmount(n("100"));
        f.getItems().add(lot);f.setPaidAmount(n("40"));long purchase=purchases.create(f).getId();em.flush();
        decimal("SELECT total_amount FROM purchase_order WHERE id=?",purchase,"100");
        decimal("SELECT qty_on_hand FROM v_stock WHERE product_id=?",p,"3");
        var bags=new PurchaseForm();var bagRow=row(p,"999","kg","20");bagRow.setEntryMode("BAGS");bagRow.setBagWeights("1.25\n2.75");
        bags.getItems().add(bagRow);purchases.create(bags);em.flush();em.clear();
        long sale=service.create(sale(p,"4","50"));
        decimal("SELECT cost_amount FROM outbound_order WHERE id=?",sale,"120");
        decimal("SELECT qty_on_hand FROM v_stock WHERE product_id=?",p,"3");
        decimal("SELECT avg_cost FROM product WHERE id=?",p,"20");
        assertThatThrownBy(() -> service.create(sale(p,"3.001","50"))).isInstanceOf(IllegalArgumentException.class);
        var invalid=sale(p,"1","50");invalid.getItems().get(0).setUnit("bao");
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("kg");
        long finalSale=service.create(sale(p,"3","50"));
        decimal("SELECT cost_amount FROM outbound_order WHERE id=?",finalSale,"60");
        decimal("SELECT qty_on_hand FROM v_stock WHERE product_id=?",p,"0");
        mvc.perform(get("/products/"+p+"/edit")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<h6>Quy đổi đơn vị"))));
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/purchases/new")).andExpect(status().isOk());
        mvc.perform(get("/purchases")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("1.25\n2.75")));
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
        mvc.perform(get("/capital?section=receipts")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khách hàng: Khách kiểm thử")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thu khách · phiếu xuất #"+id)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Trả NCC · phiếu nhập #"))));
        mvc.perform(get("/capital?section=payments")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trả NCC · phiếu nhập #"+purchase)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Thu khách · phiếu xuất #"))));
        mvc.perform(get("/capital?section=debts")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Phiếu xuất bán #"+id+"<"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Phiếu nhập hàng #"+purchase+"<"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Đã thanh toán đủ"))));
        mvc.perform(get("/capital?section=unknown")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Vốn trong kho:"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Tổng tiền phiếu:"))));
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
    @Test void xoaNhaCungCapGiuLichSuVaChanNhapMoi() throws Exception {
        long p=product(); long purchase=receive(p,"2","100","50");
        String name="NCC đã ẩn "+UUID.randomUUID();
        long supplier=jdbc.queryForObject("INSERT INTO supplier(name) VALUES (?) RETURNING id",Long.class,name);
        jdbc.update("UPDATE purchase_order SET supplier_id=? WHERE id=?",supplier,purchase);
        mvc.perform(post("/suppliers/"+supplier+"/delete")).andExpect(status().isForbidden());
        mvc.perform(post("/suppliers/"+supplier+"/delete").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/suppliers"));
        org.springframework.security.core.context.SecurityContextHolder.setContext(org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        assertThat(jdbc.queryForObject("SELECT active FROM supplier WHERE id=?",Boolean.class,supplier)).isFalse();
        assertThat(service.debts()).anySatisfy(row -> {
            assertThat(row.get("partner")).isEqualTo(name);
            assertThat((BigDecimal)row.get("remaining")).isEqualByComparingTo("150");
        });
        service.pay("purchase",purchase,n("150"),UUID.randomUUID(),"Trả nợ sau khi ẩn");
        assertThat(service.payments(false,"","",name)).hasSize(2);
        var form=new PurchaseForm();form.setSupplierId(supplier);form.getItems().add(row(p,"1","kg","100"));
        assertThatThrownBy(() -> purchases.create(form)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Hãy chọn lại");
        suppliers.delete(supplier);
        assertThatThrownBy(() -> suppliers.delete(-1)).isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/suppliers")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(name))));
        mvc.perform(get("/purchases/new")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(name))));
        mvc.perform(get("/purchases")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(name)));
    }
    @Test void timNhaCungCapTheoTenHoacDienThoaiVaTrangThai() throws Exception {
        String marker=UUID.randomUUID().toString();
        long active=jdbc.queryForObject("INSERT INTO supplier(name,phone) VALUES (?,?) RETURNING id",Long.class,"NCC Ánh "+marker,"0987654321");
        long deleted=jdbc.queryForObject("INSERT INTO supplier(name,phone,active) VALUES (?,?,false) RETURNING id",Long.class,"NCC Ánh đã xóa "+marker,"0987654321");
        long noPhone=jdbc.queryForObject("INSERT INTO supplier(name) VALUES (?) RETURNING id",Long.class,"NCC 100%_ "+marker);
        assertThat(suppliers.search(true,"ánh "+marker)).extracting(com.vatlieu.entity.Supplier::getId).containsExactly(active);
        assertThat(suppliers.search(true,"ANH "+marker)).extracting(com.vatlieu.entity.Supplier::getId).containsExactly(active);
        assertThat(suppliers.search(false,"anh da xoa "+marker)).extracting(com.vatlieu.entity.Supplier::getId).containsExactly(deleted);
        assertThat(suppliers.search(true,"A\u0301nh "+marker)).extracting(com.vatlieu.entity.Supplier::getId).containsExactly(active);
        assertThat(suppliers.search(false,marker)).extracting(com.vatlieu.entity.Supplier::getId).containsExactly(deleted);
        assertThat(suppliers.search(true,"765432")).extracting(com.vatlieu.entity.Supplier::getId).contains(active).doesNotContain(deleted,noPhone);
        assertThat(suppliers.search(false,"765432")).extracting(com.vatlieu.entity.Supplier::getId).contains(deleted).doesNotContain(active);
        assertThat(suppliers.search(true,"100%_ "+marker)).extracting(com.vatlieu.entity.Supplier::getId).containsExactly(noPhone);
        assertThat(suppliers.search(true,"không có "+marker)).isEmpty();
        assertThat(suppliers.search(true,"")).extracting(com.vatlieu.entity.Supplier::getId).contains(active,noPhone).doesNotContain(deleted);
        mvc.perform(get("/suppliers").param("keyword","  ANH "+marker+"  "))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("NCC Ánh "+marker)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("NCC Ánh đã xóa "+marker))));
        mvc.perform(get("/suppliers").param("deleted","true").param("keyword","anh da xoa "+marker))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("NCC Ánh đã xóa "+marker)));
        mvc.perform(get("/suppliers").param("keyword","không có "+marker)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Không tìm thấy nhà cung cấp phù hợp")));
    }
    @Test void suaNhaCungCapGiuLienKetVaKiemTraDuLieu() throws Exception {
        long p=product(); long purchase=receive(p,"1","100","0");
        long supplier=jdbc.queryForObject("INSERT INTO supplier(name,active) VALUES ('Tên cũ',false) RETURNING id",Long.class);
        jdbc.update("UPDATE purchase_order SET supplier_id=? WHERE id=?",supplier,purchase);
        suppliers.edit(supplier,"  Tên mới <A>  ","  0901234567  ");
        assertThat(jdbc.queryForObject("SELECT name FROM supplier WHERE id=?",String.class,supplier)).isEqualTo("Tên mới <A>");
        assertThat(jdbc.queryForObject("SELECT phone FROM supplier WHERE id=?",String.class,supplier)).isEqualTo("0901234567");
        assertThat(jdbc.queryForObject("SELECT active FROM supplier WHERE id=?",Boolean.class,supplier)).isFalse();
        assertThat(service.debts()).anySatisfy(row -> {
            assertThat(row.get("partner")).isEqualTo("Tên mới <A>");
            assertThat((BigDecimal)row.get("remaining")).isEqualByComparingTo("100");
        });
        assertThatThrownBy(() -> suppliers.edit(supplier,"   ","")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suppliers.edit(supplier,"A".repeat(151),"")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suppliers.edit(supplier,"Tên mới","1".repeat(31))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suppliers.edit(-1,"Tên mới","")).isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/suppliers?deleted=true")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tên mới &lt;A&gt;")));
        mvc.perform(post("/suppliers/"+supplier+"/edit").param("name","Giả mạo")).andExpect(status().isForbidden());
        mvc.perform(post("/suppliers/"+supplier+"/edit").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("name","Tên đã sửa").param("phone"," ").param("deleted","true"))
                .andExpect(redirectedUrl("/suppliers?deleted=true"));
        assertThat(jdbc.queryForObject("SELECT phone FROM supplier WHERE id=?",String.class,supplier)).isNull();
        mvc.perform(post("/suppliers/"+supplier+"/edit").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("name"," ").param("phone","123"))
                .andExpect(flash().attributeExists("error")).andExpect(flash().attribute("editPhone","123"));
    }
    @Test void khoiPhucNhaCungCapDeNhapHang() throws Exception {
        long p=product();
        String name="NCC khôi phục "+UUID.randomUUID();
        long supplier=jdbc.queryForObject("INSERT INTO supplier(name,active) VALUES (?,false) RETURNING id",Long.class,name);
        mvc.perform(get("/suppliers?deleted=true")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(name)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Khôi phục")));
        mvc.perform(post("/suppliers/"+supplier+"/restore")).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)
            mvc.perform(post("/suppliers/"+supplier+"/restore").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                    .andExpect(redirectedUrl("/suppliers?deleted=true"));
        mvc.perform(get("/suppliers?deleted=true")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(name))));
        mvc.perform(get("/suppliers")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(name)));
        mvc.perform(get("/purchases/new")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(name)));
        mvc.perform(post("/suppliers/-1/restore").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(flash().attribute("error","Nhà cung cấp không còn tồn tại"));
        org.springframework.security.core.context.SecurityContextHolder.setContext(org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        var form=new PurchaseForm();form.setSupplierId(supplier);form.getItems().add(row(p,"1","kg","100"));
        assertThat(purchases.create(form).getSupplier().getId()).isEqualTo(supplier);
    }
    @Test void locLichSuTheoNgayVaTen() throws Exception {
        long p=product(); long purchase=receive(p,"2","100","50");
        long supplier=jdbc.queryForObject("INSERT INTO supplier(name) VALUES ('NCC lọc lịch sử') RETURNING id", Long.class);
        jdbc.update("UPDATE purchase_order SET supplier_id=? WHERE id=?", supplier, purchase);
        var f=sale(p,"1","200"); f.setCustomer("Khách Lọc 100%_A"); f.setPaidAmount(n("100"));
        long id=service.create(f);
        jdbc.update("UPDATE cash_payment SET created_at=TIMESTAMP '2026-10-20 00:00:00' WHERE outbound_id=?",id);
        jdbc.update("UPDATE cash_payment SET created_at=TIMESTAMP '2026-10-20 23:59:59.999999' WHERE purchase_id=?",purchase);
        assertThat(service.payments(true,"2026-10-20","2026-10-20","  khách lọc  "))
                .anySatisfy(row -> assertThat(((Number)row.get("outbound_id")).longValue()).isEqualTo(id));
        assertThat(service.payments(false,"2026-10-20","2026-10-20","ncc lọc lịch sử"))
                .anySatisfy(row -> assertThat(((Number)row.get("purchase_id")).longValue()).isEqualTo(purchase));
        assertThat(service.payments(true,"2026-10-21","","Khách Lọc 100%_A")).isEmpty();
        assertThat(service.payments(false,"","2026-10-19","NCC lọc lịch sử")).isEmpty();
        assertThat(service.payments(true,"","","100%_A")).hasSize(1);
        assertThat(service.payments(true,"","","100%_B")).isEmpty();
        assertThatThrownBy(() -> service.payments(true,"2026-10-21","2026-10-20",""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Từ ngày");
        assertThatThrownBy(() -> service.payments(true,"sai-ngay","",""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không hợp lệ");
        mvc.perform(get("/capital").param("section","receipts").param("from","2026-10-20").param("to","2026-10-20").param("partner","Khách Lọc"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Thu khách · phiếu xuất #"+id)));
        mvc.perform(get("/capital?section=payments&from=2026-10-21&to=2026-10-20"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Từ ngày phải trước hoặc bằng đến ngày")));
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
