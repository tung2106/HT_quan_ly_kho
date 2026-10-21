package com.vatlieu.web;

import com.vatlieu.entity.Product;
import com.vatlieu.entity.UnitConversion;
import com.vatlieu.form.ProductForm;
import com.vatlieu.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepo;
    private final UnitConversionRepository convRepo;

    @GetMapping
    public String list(Model m) {
        m.addAttribute("products", productRepo.findAllByOrderByNameAsc());
        return "products";
    }

    @GetMapping("/new")
    public String create(Model m) {
        m.addAttribute("form", new ProductForm());
        m.addAttribute("categories", categoryRepo.findAllByOrderByNameAsc());
        return "product-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        Product p = productRepo.findById(id).orElseThrow();
        ProductForm f = new ProductForm();
        f.setId(p.getId());
        f.setCategoryId(p.getCategory().getId());
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
        m.addAttribute("categories", categoryRepo.findAllByOrderByNameAsc());
        return "product-form";
    }

    @PostMapping
    public String save(@ModelAttribute ProductForm f, RedirectAttributes ra) {
        try {
            // Sửa: nạp lại bản ghi cũ để KHÔNG ghi đè giá vốn bình quân (avgCost)
            Product p = f.getId() == null ? new Product() : productRepo.findById(f.getId()).orElseThrow();
            p.setCategory(categoryRepo.getReferenceById(f.getCategoryId()));
            p.setCode(f.getCode() == null || f.getCode().isBlank() ? null : f.getCode().trim());
            p.setName(f.getName().trim());
            p.setBaseUnit(f.getBaseUnit().trim());
            p.setMinStock(f.getMinStock() == null ? BigDecimal.ZERO : f.getMinStock());
            p.setRetailPrice(f.getRetailPrice() == null ? BigDecimal.ZERO : f.getRetailPrice());
            p.setActive(f.isActive());
            p.setNote(f.getNote());
            productRepo.save(p);
            ra.addFlashAttribute("msg", "Đã lưu sản phẩm");
            return "redirect:/products/" + p.getId() + "/edit";
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Mã sản phẩm bị trùng");
            return f.getId() == null ? "redirect:/products/new" : "redirect:/products/" + f.getId() + "/edit";
        }
    }

    @PostMapping("/{id}/units")
    public String addUnit(@PathVariable Long id, @RequestParam String unitName,
                          @RequestParam BigDecimal factor, RedirectAttributes ra) {
        if (factor.signum() <= 0) {
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
