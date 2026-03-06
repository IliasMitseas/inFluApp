package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.InfluencerDashboardDto;
import org.ilias.influapp.services.DashboardService;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class InfluencerDashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping("/influencer/dashboard")
    public String influencerDashboard(Authentication authentication, Model model) {

        var user = userService.currentUser(authentication);
        Long influencerId = user.getId();
        InfluencerDashboardDto dto = dashboardService.getInfluencerDashboard(influencerId, null, null);
        model.addAttribute("dashboard", dto);

        return "influencer-dashboard";
    }
}
