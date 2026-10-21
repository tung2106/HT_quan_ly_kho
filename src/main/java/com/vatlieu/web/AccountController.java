package com.vatlieu.web;

import com.vatlieu.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequiredArgsConstructor
public class AccountController {
    private final AccountService accounts;
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/error/403") public String forbidden() { return "forbidden"; }
    @GetMapping("/account/password") public String password() { return "password"; }
    @PostMapping("/account/password") public String change(@RequestParam String oldPassword,@RequestParam String newPassword,@RequestParam String confirm,HttpServletRequest request,Model model) throws jakarta.servlet.ServletException {
        try { accounts.changePassword(oldPassword,newPassword,confirm);request.logout();request.getSession().invalidate();return "redirect:/login?changed"; }
        catch(IllegalArgumentException e) { model.addAttribute("error",e.getMessage());return "password"; }
    }
    @GetMapping("/admin/users") public String users(Model model) { model.addAttribute("users",accounts.list());return "users"; }
    @PostMapping("/admin/users") public String create(@RequestParam String username,@RequestParam String fullName,@RequestParam String role,@RequestParam String password,RedirectAttributes flash) {
        try { accounts.create(username,fullName,role,password);flash.addFlashAttribute("msg","Đã cấp tài khoản; người dùng phải đổi mật khẩu lần đầu"); }
        catch(IllegalArgumentException | org.springframework.dao.DuplicateKeyException e) { flash.addFlashAttribute("error",e instanceof IllegalArgumentException?e.getMessage():"Tên đăng nhập đã tồn tại"); }
        return "redirect:/admin/users";
    }
    @PostMapping("/admin/users/{id}/active") public String active(@PathVariable long id,@RequestParam boolean active,RedirectAttributes flash) {
        try { accounts.setActive(id,active);flash.addFlashAttribute("msg","Đã cập nhật trạng thái tài khoản"); }
        catch(IllegalArgumentException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/users";
    }
    @PostMapping("/admin/users/{id}/password") public String reset(@PathVariable long id,@RequestParam String password,RedirectAttributes flash) {
        try { accounts.resetPassword(id,password);flash.addFlashAttribute("msg","Đã đặt lại mật khẩu và kết thúc các phiên đăng nhập cũ"); }
        catch(IllegalArgumentException e) { flash.addFlashAttribute("error",e.getMessage()); }
        return "redirect:/admin/users";
    }
}
