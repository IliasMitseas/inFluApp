package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final InfluencerRepository influencerRepository;
    private final BusinessRepository businessRepository;

    @GetMapping("/influencer/home")
    public String influencerHome(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId())
                .orElseThrow(NotFoundException::new);
        model.addAttribute("influencer", influencer);
        return "influencer-home";
    }

    @GetMapping("/business/home")
    public String businessHome(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        Business business = businessRepository.findById(user.getId())
                .orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        return "business-home";
    }

    @GetMapping("/influencer/profile")
    public String influencerProfile(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId())
                .orElseThrow(NotFoundException::new);
        model.addAttribute("influencer", influencer);
        return "influencer-profile";
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

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    private static class UnauthorizedException extends RuntimeException {
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class NotFoundException extends RuntimeException {
    }
}
