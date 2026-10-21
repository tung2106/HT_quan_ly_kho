package com.vatlieu.web;

import com.vatlieu.entity.Supplier;
import com.vatlieu.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/suppliers")
public class SupplierController {
    private final SupplierRepository repo;
    private final com.vatlieu.service.SupplierService service;

    @GetMapping
    public String list(Model m) {
        m.addAttribute("suppliers", repo.findByActiveTrueOrderByNameAsc());
        return "suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        try { service.delete(id); ra.addFlashAttribute("msg", "Đã xóa nhà cung cấp khỏi danh sách chọn. Lịch sử và công nợ vẫn được giữ nguyên."); }
        catch (IllegalArgumentException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/suppliers";
    }

    @PostMapping
    public String add(@RequestParam String name, @RequestParam(required = false) String phone,
                      RedirectAttributes ra) {
        Supplier s = new Supplier();
        s.setName(name.trim());
        s.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        repo.save(s);
        ra.addFlashAttribute("msg", "Đã thêm nhà cung cấp");
        return "redirect:/suppliers";
    }
}
