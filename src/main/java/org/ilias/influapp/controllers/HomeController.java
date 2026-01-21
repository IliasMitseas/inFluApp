package org.ilias.influapp.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;

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
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("influencer", influencer);

        // Backing object for platform checkbox selection
        ProfilePlatformsForm platformsForm = new ProfilePlatformsForm();
        if (influencer.getSocialMediaAccounts() != null) {
            for (SocialMedia sm : influencer.getSocialMediaAccounts()) {
                if (sm != null && sm.getPlatform() != null) {
                    platformsForm.getSelectedPlatforms().add(sm.getPlatform());
                }
            }
        }
        model.addAttribute("platformsForm", platformsForm);
        model.addAttribute("allPlatforms", Platform.values());

        return "influencer-profile";
    }

    @PostMapping("/influencer/profile/platforms")
    public String updateInfluencerPlatforms(Authentication authentication, @ModelAttribute("platformsForm") ProfilePlatformsForm platformsForm) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        EnumSet<Platform> selected = platformsForm == null || platformsForm.getSelectedPlatforms() == null
                ? EnumSet.noneOf(Platform.class) : EnumSet.copyOf(platformsForm.getSelectedPlatforms());

        // Remove unchecked
        influencer.getSocialMediaAccounts().removeIf(sm -> sm != null && sm.getPlatform() != null && !selected.contains(sm.getPlatform()));

        // Add missing
        for (Platform p : selected) {
            boolean exists = influencer.getSocialMediaAccounts().stream().anyMatch(sm -> sm != null && p.equals(sm.getPlatform()));
            if (!exists) {
                SocialMedia sm = new SocialMedia();
                sm.setInfluencer(influencer);
                sm.setPlatform(p);
                sm.setAccountUrl("pending://" + p.name().toLowerCase());
                sm.setFollowers(0);
                influencer.getSocialMediaAccounts().add(sm);
            }
        }
        influencerRepository.save(influencer);
        return "redirect:/influencer/profile";
    }

    @PostMapping("/influencer/profile")
    public String updateInfluencerProfile(Authentication authentication, @ModelAttribute("influencer") Influencer updateInfluencer) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        influencer.setName(updateInfluencer.getName());
        influencer.setCategory(updateInfluencer.getCategory());
        influencer.setInfluencerType(updateInfluencer.getInfluencerType());
        influencer.setTotalFollowers(updateInfluencer.getTotalFollowers());
        influencer.setEngagementRate(updateInfluencer.getEngagementRate());

        influencerRepository.save(influencer);

        return "redirect:/influencer/profile";
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

    @Data
    public static class ProfilePlatformsForm {
        private EnumSet<Platform> selectedPlatforms = EnumSet.noneOf(Platform.class);
    }
}
