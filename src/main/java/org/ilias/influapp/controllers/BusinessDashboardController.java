package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.BusinessDashboardDto;
import org.ilias.influapp.services.DashboardService;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class BusinessDashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping("/business/dashboard")
    public String businessDashboard(Authentication authentication, Model model) {
        var user = userService.currentUser(authentication);
        Long businessId = user.getId();
        BusinessDashboardDto dto = dashboardService.getBusinessDashboard(businessId, null, null);

        model.addAttribute("dashboard", dto);

        return "business-dashboard";
    }
}

