package io.github.junhyeong9812.overload.starter.controller;

import io.github.junhyeong9812.overload.starter.OverloadProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard UI controller.
 *
 * @author junhyeong9812
 * @since 1.0.0
 */
@Controller
public class OverloadDashboardController {

  private final OverloadProperties properties;

  public OverloadDashboardController(OverloadProperties properties) {
    this.properties = properties;
  }

  @GetMapping("${overload.dashboard.path:/overload}")
  public String dashboard(Model model) {
    model.addAttribute("title", properties.getDashboard().getTitle());
    model.addAttribute("basePath", properties.getDashboard().getPath());
    model.addAttribute("defaults", properties.getDefaults());
    return "overload/dashboard";
  }
}