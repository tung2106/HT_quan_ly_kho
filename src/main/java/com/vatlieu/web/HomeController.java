package com.vatlieu.web;

import com.vatlieu.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final StockService stockService;

    @GetMapping("/")
    public String home() {
        return "redirect:/stock";
    }

    @GetMapping("/stock")
    public String stock(Model m) {
        m.addAttribute("rows", stockService.findAll());
        return "stock";
    }
}
