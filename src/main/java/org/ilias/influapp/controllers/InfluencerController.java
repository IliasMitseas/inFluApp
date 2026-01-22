package org.ilias.influapp.controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.exceptions.UnauthorizedException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

@Controller
@RequiredArgsConstructor
public class InfluencerController {

    private final UserRepository userRepository;
    private final InfluencerRepository influencerRepository;

    @GetMapping("/influencer/home")
    public String influencerHome(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId())
                .orElseThrow(NotFoundException::new);
        model.addAttribute("influencer", influencer);
        return "influencer-home";
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
        influencer.setAge(updateInfluencer.getAge());
        influencer.setLocation(updateInfluencer.getLocation());
        influencer.setBio(updateInfluencer.getBio());
        influencer.setIsAvailable(updateInfluencer.getIsAvailable());
        influencer.setMinCollaborationBudget(updateInfluencer.getMinCollaborationBudget());
        influencer.setCategory(updateInfluencer.getCategory());
        influencer.setInfluencerType(updateInfluencer.getInfluencerType());
        influencer.setTotalFollowers(updateInfluencer.getTotalFollowers());
        influencer.setEngagementRate(updateInfluencer.getEngagementRate());
        influencerRepository.save(influencer);
        return "redirect:/influencer/profile";
    }

    @PostMapping("/influencer/profile/image")
    public String uploadProfileImage(Authentication authentication, @RequestParam("file") MultipartFile file) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        if (file == null || file.isEmpty()) {
            return "redirect:/influencer/profile";
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return "redirect:/influencer/profile";
        }

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safe = original.replaceAll("[^a-zA-Z0-9.\\-_/]", "_");
        String filename = "influencer-" + influencer.getId() + "-" + System.currentTimeMillis() + "-" + safe;

        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            if(!target.startsWith(uploadDir)) {
                return "redirect:/influencer/profile";
            }
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        influencer.setImageUrl("/uploads/" + filename);
        influencerRepository.save(influencer);
        return "redirect:/influencer/profile";
    }

    @GetMapping("/influencer/social/{platform}")
    public String influencerSocialPlatform(Authentication authentication, @PathVariable("platform") Platform platform, Model model) {
        User user = currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = null;
        if (influencer.getSocialMediaAccounts() != null) {
            socialMedia = influencer.getSocialMediaAccounts().stream()
                    .filter(sm -> sm != null && platform.equals(sm.getPlatform()))
                    .findFirst()
                    .orElse(null);
        }

        model.addAttribute("influencer", influencer);
        model.addAttribute("platform", platform);
        model.addAttribute("socialMedia", socialMedia);
        return "influencer-platform";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException();
        }
        String login = authentication.getName();
        return userRepository.findByEmailOrUsername(login, login).orElseThrow(UnauthorizedException::new);
    }

    @Data
    public static class ProfilePlatformsForm {
        private EnumSet<Platform> selectedPlatforms = EnumSet.noneOf(Platform.class);
    }
}
