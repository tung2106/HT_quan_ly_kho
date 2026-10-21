package com.vatlieu.web;

import com.vatlieu.form.PurchaseForm;
import com.vatlieu.form.PurchaseItemForm;
import com.vatlieu.repository.ProductRepository;
import com.vatlieu.repository.PurchaseOrderRepository;
import com.vatlieu.repository.SupplierRepository;
import com.vatlieu.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/purchases")
public class PurchaseController {
    private final PurchaseService service;
    private final com.vatlieu.service.UnitOptionsService units;
    private final ProductRepository productRepo;
    private final SupplierRepository supplierRepo;
    private final PurchaseOrderRepository orderRepo;

    @GetMapping
    public String list(Model m) {
        m.addAttribute("orders", orderRepo.findAllByOrderByOrderDateDescIdDesc());
        return "purchases";
    }

    @GetMapping("/new")
    public String form(Model m) {
        PurchaseForm f = new PurchaseForm();
        f.setOrderDate(LocalDate.now());
        f.getItems().add(new PurchaseItemForm());
        m.addAttribute("form", f);
        populate(m);
        return "purchase-form";
    }

    @PostMapping
    public String save(@ModelAttribute("form") PurchaseForm f, BindingResult binding, Model m, RedirectAttributes ra) {
        try {
            if (binding.hasErrors()) throw new IllegalArgumentException("Ngày, số lượng hoặc số tiền không hợp lệ");
            service.create(f);
            ra.addFlashAttribute("msg", "Đã lưu đơn nhập hàng");
            return "redirect:/purchases";
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
            populate(m);
            return "purchase-form";
        }
    }

    private void populate(Model m) {
        m.addAttribute("unitOptions", units.findAll());
        m.addAttribute("products", productRepo.findByActiveTrueOrderByNameAsc());
        m.addAttribute("suppliers", supplierRepo.findByActiveTrueOrderByNameAsc());
    }
}
