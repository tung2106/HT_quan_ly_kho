package com.vatlieu.web;

import com.vatlieu.entity.Product;
import com.vatlieu.entity.UnitConversion;
import com.vatlieu.form.ProductForm;
import com.vatlieu.repository.ProductRepository;
import com.vatlieu.repository.UnitConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository productRepo;
    private final UnitConversionRepository convRepo;
    private final com.vatlieu.service.ProductService service;

    @GetMapping
    public String list(Model m) {
        m.addAttribute("products", productRepo.findAllByOrderByNameAsc());
        return "products";
    }

    @GetMapping("/new")
    public String create(Model m) {
        m.addAttribute("form", new ProductForm());
        return "product-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        Product p = productRepo.findById(id).orElseThrow();
        ProductForm f = new ProductForm();
        f.setId(p.getId());
        f.setProductGroup(p.getProductGroup());
        f.setCode(p.getCode());
        f.setName(p.getName());
        f.setBaseUnit(p.getBaseUnit());
        f.setMinStock(p.getMinStock());
        f.setRetailPrice(p.getRetailPrice());
        f.setActive(p.isActive());
        f.setNote(p.getNote());
        m.addAttribute("form", f);
        m.addAttribute("product", p);
        m.addAttribute("units", convRepo.findByProductIdOrderByUnitNameAsc(id));
        return "product-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        try { service.delete(id); ra.addFlashAttribute("msg", "Đã ngừng nhập/bán sản phẩm. Tồn kho, vốn và lịch sử vẫn được giữ nguyên."); }
        catch (IllegalArgumentException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/products";
    }

    @PostMapping
    public String save(@ModelAttribute ProductForm f, RedirectAttributes ra) {
        try {
            Product p = service.save(f);
            ra.addFlashAttribute("msg", "Đã lưu sản phẩm");
            return "redirect:/products/" + p.getId() + "/edit";
        } catch (DataIntegrityViolationException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e instanceof IllegalArgumentException ? e.getMessage() : "Mã sản phẩm bị trùng hoặc dữ liệu không hợp lệ");
            return f.getId() == null ? "redirect:/products/new" : "redirect:/products/" + f.getId() + "/edit";
        }
    }

    @PostMapping("/{id}/units")
    public String addUnit(@PathVariable Long id, @RequestParam String unitName,
                          @RequestParam BigDecimal factor, RedirectAttributes ra) {
        if (productRepo.findById(id).orElseThrow().isAgriculture()) {
            ra.addFlashAttribute("error", "Nông sản dùng kg cân thực tế, không dùng quy đổi bao cố định");
        } else if (factor.signum() <= 0) {
            ra.addFlashAttribute("error", "Hệ số quy đổi phải > 0");
        } else {
            UnitConversion u = new UnitConversion();
            u.setProduct(productRepo.getReferenceById(id));
            u.setUnitName(unitName.trim());
            u.setFactor(factor);
            try {
                convRepo.save(u);
                ra.addFlashAttribute("msg", "Đã thêm quy đổi");
            } catch (DataIntegrityViolationException e) {
                ra.addFlashAttribute("error", "Đơn vị này đã có quy đổi");
            }
        }
        return "redirect:/products/" + id + "/edit";
    }

    @PostMapping("/{id}/units/{uid}/delete")
    public String deleteUnit(@PathVariable Long id, @PathVariable Long uid) {
        convRepo.deleteById(uid);
        return "redirect:/products/" + id + "/edit";
    }
}
