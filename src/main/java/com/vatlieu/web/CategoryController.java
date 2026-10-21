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
    private final com.vatlieu.service.CategoryService service;

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
            service.save(null,name,perishable);
            ra.addFlashAttribute("msg", "Đã thêm danh mục");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Danh mục đã tồn tại");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error",e.getMessage());
        }
        return "redirect:/categories";
    }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable long id,@RequestParam String name,@RequestParam(defaultValue="false") boolean perishable,RedirectAttributes ra) {
        try { service.save(id,name,perishable);ra.addFlashAttribute("msg","Đã sửa danh mục"); }
        catch(DataIntegrityViolationException e) { ra.addFlashAttribute("error","Tên danh mục đã tồn tại"); }
        catch(IllegalArgumentException e) { ra.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/categories";
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id,RedirectAttributes ra) {
        try { service.delete(id);ra.addFlashAttribute("msg","Đã xóa danh mục"); }
        catch(DataIntegrityViolationException e) { ra.addFlashAttribute("error","Danh mục đang được sử dụng, không thể xóa"); }
        catch(IllegalArgumentException e) { ra.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/categories";
    }
}
