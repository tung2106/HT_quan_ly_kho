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
    public String list(@RequestParam(defaultValue="false") boolean deleted,
                       @RequestParam(defaultValue="") String keyword, Model m) {
        m.addAttribute("deleted", deleted);
        m.addAttribute("keyword", keyword.trim());
        m.addAttribute("suppliers", service.search(!deleted, keyword));
        return "suppliers";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable long id, @RequestParam String name,
                       @RequestParam(defaultValue="") String phone,
                       @RequestParam(defaultValue="false") boolean deleted, RedirectAttributes ra) {
        try { service.edit(id, name, phone); ra.addFlashAttribute("msg", "Đã cập nhật nhà cung cấp"); }
        catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("editId", id);
            ra.addFlashAttribute("editName", name);
            ra.addFlashAttribute("editPhone", phone);
        }
        return deleted ? "redirect:/suppliers?deleted=true" : "redirect:/suppliers";
    }

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable long id, RedirectAttributes ra) {
        try { service.restore(id); ra.addFlashAttribute("msg", "Đã khôi phục nhà cung cấp. Bạn có thể chọn khi nhập hàng mới."); }
        catch (IllegalArgumentException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/suppliers?deleted=true";
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
