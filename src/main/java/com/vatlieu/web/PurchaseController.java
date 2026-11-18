package com.vatlieu.web;

import com.vatlieu.form.PurchaseForm;
import com.vatlieu.form.PurchaseItemForm;
import com.vatlieu.repository.ProductRepository;
import com.vatlieu.service.PurchaseService;
import com.vatlieu.service.PurchaseTrashService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/purchases")
public class PurchaseController {

  private final PurchaseService service;
  private final PurchaseTrashService trashService;
  private final ProductRepository productRepo;

  @GetMapping
  public String list(
    @RequestParam(defaultValue = "") String keyword,
    @RequestParam(defaultValue = "false") boolean trash,
    Model m
  ) {
    m.addAttribute("orders", service.history(keyword, trash));
    m.addAttribute("trash", trash);
    m.addAttribute("keyword", keyword);
    return "purchases/list";
  }

  @GetMapping("/new")
  public String form(Model m) {
    PurchaseForm f = new PurchaseForm();
    f.setOrderDate(LocalDate.now());
    f.setDirectEntry(true);
    f.setLotCode(service.nextLotCode());
    f.getItems().add(new PurchaseItemForm());
    m.addAttribute("form", f);
    populate(m);
    return "purchases/form";
  }

  @PostMapping("/{id}/{action:trash|restore}")
  public String changeTrash(
    @PathVariable long id,
    @PathVariable String action,
    @RequestParam(defaultValue = "") String keyword,
    RedirectAttributes ra
  ) {
    boolean trash = action.equals("trash");
    try {
      trashService.setTrashed(id, trash);
      ra.addFlashAttribute(
        "msg",
        trash ? "Đã chuyển phiếu nhập vào Thùng rác." : "Đã khôi phục phiếu nhập."
      );
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    if (!trash) ra.addAttribute("trash", true);
    if (!keyword.isBlank()) ra.addAttribute("keyword", keyword);
    return "redirect:/purchases";
  }

  @PostMapping
  public String save(
    @ModelAttribute("form") PurchaseForm f,
    BindingResult binding,
    Model m,
    RedirectAttributes ra
  ) {
    try {
      if (binding.hasErrors()) throw new IllegalArgumentException(
        "Ngày, số lượng hoặc số tiền không hợp lệ"
      );
      service.create(f);
      ra.addFlashAttribute("msg", "Đã lưu đơn nhập hàng");
      return "redirect:/purchases";
    } catch (IllegalArgumentException e) {
      m.addAttribute("error", e.getMessage());
      populate(m);
      return "purchases/form";
    }
  }

  private void populate(Model m) {
    var available = productRepo.findByActiveTrueOrderByNameAsc();
    m.addAttribute("products", available);
    var baseUnits = new java.util.LinkedHashSet<>(
      java.util.List.of("kg", "viên", "bao", "m3", "m2", "m", "cái", "tấm", "cây")
    );
    available.forEach(p -> baseUnits.add(p.getBaseUnit()));
    m.addAttribute("baseUnits", baseUnits);
    PurchaseForm form = (PurchaseForm) m.getAttribute("form");
    if (form.getItems() == null) form.setItems(new java.util.ArrayList<>());
    if (form.getItems().isEmpty()) form.getItems().add(new PurchaseItemForm());
    if (!form.isDirectEntry() && form.getItems().get(0).getProductId() != null) {
      available
        .stream()
        .filter(p -> p.getId().equals(form.getItems().get(0).getProductId()))
        .findFirst()
        .ifPresent(p -> {
          form.setProductName(p.getName());
          form.setProductGroup(p.getProductGroup());
          form.setBaseUnit(p.getBaseUnit());
        });
    }
  }
}
