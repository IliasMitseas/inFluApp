package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessRepository businessRepository;
    private final UserService userService;

    @GetMapping("/business/home")
    public String businessHome(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        Business business = businessRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        return "business-home";
    }

    @GetMapping("/business/profile")
    public String businessProfile(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        Business business = businessRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        return "business-profile";
    }
}
