package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.exceptions.UnauthorizedException;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class BusinessController {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;

    @GetMapping("/business/home")
    public String businessHome(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        Business business = businessRepository.findById(user.getId())
                .orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        return "business-home";
    }

    @GetMapping("/business/profile")
    public String businessProfile(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        Business business = businessRepository.findById(user.getId())
                .orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        return "business-profile";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException();
        }
        String login = authentication.getName();
        return userRepository.findByEmailOrUsername(login, login).orElseThrow(UnauthorizedException::new);
    }
}
