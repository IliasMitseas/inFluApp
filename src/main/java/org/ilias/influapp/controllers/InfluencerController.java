package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.ProfilePlatformsForm;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.services.InfluencerServiceImpl;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class InfluencerController {

    private final InfluencerRepository influencerRepository;
    private final InfluencerServiceImpl influencerService;
    private final UserService userService;

    @GetMapping("/influencer/home")
    public String influencerHome(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("influencer", influencer);
        return "influencer-home";
    }

    @GetMapping("/influencer/profile")
    public String influencerProfile(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        model.addAttribute("influencer", influencer);
        model.addAttribute("platformsForm", influencerService.getProfilePlatformsForm(user.getId()));
        model.addAttribute("allPlatforms", Platform.values());
        return "influencer-profile";
    }

    @PostMapping("/influencer/profile")
    public String updateInfluencerProfile(Authentication authentication, @ModelAttribute("influencer") Influencer updateInfluencer) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerService.updateInfluencer(updateInfluencer, user);
        influencerRepository.save(influencer);
        return "redirect:/influencer/home";
    }

    @PostMapping("/influencer/profile/platforms")
    public String updateInfluencerPlatforms(Authentication authentication,
                                           @ModelAttribute("platformsForm") ProfilePlatformsForm platformsForm) {
        User user = userService.currentUser(authentication);
        influencerService.updateInfluencerPlatforms(user.getId(), platformsForm);
        return "redirect:/influencer/home";
    }

    @PostMapping("/influencer/profile/image")
    public String uploadProfileImage(Authentication authentication, @RequestParam("file") MultipartFile file) {
        User user = userService.currentUser(authentication);

        try {
            influencerService.uploadProfileImage(user.getId(), file);
        } catch (IllegalArgumentException | SecurityException e) {
            // Handle validation errors (empty file, wrong type, security issues)
            return "redirect:/influencer/profile?error=" + e.getMessage();
        } catch (IOException e) {
            // Handle file system errors
            return "redirect:/influencer/profile?error=upload_failed";
        }
        return "redirect:/influencer/profile";
    }
}
