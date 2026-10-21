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

    @GetMapping
    public String list(Model m) {
        m.addAttribute("suppliers", repo.findAllByOrderByNameAsc());
        return "suppliers";
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
