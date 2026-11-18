package com.vatlieu.web;

import com.vatlieu.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

  private final StockService stockService;
  private final com.vatlieu.service.CurrentUser current;
  private final com.vatlieu.service.OperationsService operations;

  @GetMapping("/")
  public String home(Model model) {
    if (current.owner()) model.addAttribute("money", operations.summary());
    return "home";
  }

  @GetMapping("/stock/lots")
  public String lots(@RequestParam(defaultValue = "") String group, Model m) {
    m.addAttribute("selectedGroup", StockService.normalizeGroup(group));
    m.addAttribute("productLots", stockService.lotsByProduct(group));
    return "stock/lots";
  }

  @GetMapping("/stock")
  public String stock(@RequestParam(defaultValue = "") String group, Model m) {
    m.addAttribute("selectedGroup", StockService.normalizeGroup(group));
    m.addAttribute("rows", stockService.findAll(group));
    return "stock/overview";
  }
}
