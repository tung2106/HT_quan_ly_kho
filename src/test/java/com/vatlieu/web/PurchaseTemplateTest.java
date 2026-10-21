package com.vatlieu.web;

import com.vatlieu.entity.Category;
import com.vatlieu.entity.Product;
import com.vatlieu.repository.*;
import com.vatlieu.service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.security.test.context.support.WithMockUser(roles="OWNER")
@WebMvcTest(PurchaseController.class)
class PurchaseTemplateTest {
    @Autowired MockMvc mvc;
    @MockBean PurchaseService service;
    @MockBean com.vatlieu.service.UnitOptionsService units;
    @MockBean ProductRepository productRepo;
    @MockBean SupplierRepository supplierRepo;
    @MockBean PurchaseOrderRepository orderRepo;

    @Test
    void formKhongTuanTuHoaQuanHeJpaVaEscapeTenSanPham() throws Exception {
        // Quan hệ không cần cho form phải không bị truy cập khi render JavaScript.
        Product product = new Product() {
            @Override
            public Category getCategory() {
                throw new IllegalStateException("Không tuần tự hóa quan hệ JPA vào JavaScript");
            }
        };
        product.setId(1L);
        product.setName("Xi măng <PC40> & bao");
        product.setBaseUnit("kg");
        when(productRepo.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(product));

        mvc.perform(get("/purchases/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xi măng &lt;PC40&gt; &amp; bao")))
                .andExpect(content().string(not(containsString("const products ="))));
    }
}
