package com.vatlieu.web;

import com.vatlieu.form.OutboundForm;
import com.vatlieu.form.PurchaseItemForm;
import com.vatlieu.repository.ProductRepository;
import com.vatlieu.service.OperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.UUID;

@Controller @RequiredArgsConstructor
public class OperationsController {
    private final OperationsService service;
    private final com.vatlieu.service.CurrentUser current;
    private final com.vatlieu.service.UnitOptionsService units;
    private final ProductRepository products;

    @GetMapping("/outbound")
    public String list(Model model) { model.addAttribute("orders",service.orders()); return "outbound"; }
    @GetMapping("/outbound/new")
    public String form(@RequestParam(defaultValue="SALE") String kind, Model model) {
        if(!"SALE".equals(kind)) current.requireOwner();
        OutboundForm form = new OutboundForm();
        form.setKind("WASTE".equals(kind) ? "WASTE" : "SALE");
        form.getItems().add(new PurchaseItemForm());
        model.addAttribute("form",form);
        model.addAttribute("unitOptions",units.findAll());
        model.addAttribute("products",products.findByActiveTrueOrderByNameAsc());
        return "outbound-form";
    }
    @PostMapping("/outbound")
    public String save(@ModelAttribute("form") OutboundForm form, BindingResult binding, Model model) {
        try {
            if (binding.hasErrors()) throw new IllegalArgumentException("Số lượng, giá hoặc tiền đã thu không hợp lệ");
            return "redirect:/outbound/" + service.create(form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error",e.getMessage());
            model.addAttribute("unitOptions",units.findAll());
            model.addAttribute("products",products.findByActiveTrueOrderByNameAsc());
            if (form.getItems().isEmpty()) form.getItems().add(new PurchaseItemForm());
            return "outbound-form";
        }
    }
    @GetMapping("/outbound/{id}")
    public String detail(@PathVariable long id, Model model, RedirectAttributes flash) {
        try { model.addAttribute("order",service.detail(id)); return "outbound-detail"; }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("error",e.getMessage()); return "redirect:/outbound"; }
    }
    @GetMapping("/capital")
    public String capital(@RequestParam(defaultValue="capital") String section,
                          @RequestParam(defaultValue="") String from,
                          @RequestParam(defaultValue="") String to,
                          @RequestParam(defaultValue="") String partner, Model model) {
        if (!java.util.Set.of("capital", "debts", "receipts", "payments").contains(section)) section = "capital";
        model.addAttribute("section", section);
        model.addAttribute("summary",service.summary());
        var debts = service.debts();
        debts.forEach(d -> d.put("request_key",UUID.randomUUID()));
        model.addAttribute("debts",debts);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("partner", partner);
        model.addAttribute("payments", java.util.List.of());
        if ("receipts".equals(section) || "payments".equals(section)) {
            try { model.addAttribute("payments", service.payments("receipts".equals(section), from, to, partner)); }
            catch (IllegalArgumentException e) { model.addAttribute("error", e.getMessage()); }
        }
        return "capital";
    }
    @PostMapping("/capital/pay")
    public String pay(@RequestParam String type,@RequestParam long id,@RequestParam BigDecimal amount,
                      @RequestParam UUID requestKey,@RequestParam(defaultValue="") String note,RedirectAttributes flash) {
        try { service.pay(type,id,amount,requestKey,note); flash.addFlashAttribute("msg","Đã ghi thanh toán"); }
        catch (IllegalArgumentException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/capital?section=debts";
    }
}
