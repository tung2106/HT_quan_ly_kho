package com.vatlieu.web;

import com.vatlieu.entity.Category;
import com.vatlieu.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepository repo;

    @GetMapping
    public String list(Model m) {
        m.addAttribute("categories", repo.findAllByOrderByNameAsc());
        return "categories";
    }

    @PostMapping
    public String add(@RequestParam String name,
                      @RequestParam(defaultValue = "false") boolean perishable,
                      RedirectAttributes ra) {
        try {
            Category c = new Category();
            c.setName(name.trim());
            c.setPerishable(perishable);
            repo.save(c);
            ra.addFlashAttribute("msg", "Đã thêm danh mục");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Danh mục đã tồn tại");
        }
        return "redirect:/categories";
    }
}
