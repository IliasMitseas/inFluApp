package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.Enums.Platform;
import org.ilias.influapp.entities.SocialMedia;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.services.InfluencerService;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class InfluencerPlatformController {

    private final InfluencerRepository influencerRepository;
    private final InfluencerService influencerService;
    private final UserService userService;

    @GetMapping("/influencer/social/{platform}")
    public String influencerSocialPlatform(Authentication authentication, @PathVariable Platform platform, Model model) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = null;
        try {
            socialMedia = influencerService.findSocialMediaByPlatform(influencer, platform);
        } catch (NotFoundException e) {
            System.out.println("Social media platform not found for influencer: " + platform);
        }

        model.addAttribute("influencer", influencer);
        model.addAttribute("platform", platform);
        model.addAttribute("socialMedia", socialMedia);

        return "influencer-platform";
    }

    @PostMapping("/influencer/social/{platform}/edit")
    public String editInfluencerSocialPlatform(Authentication authentication,
                                               @PathVariable Platform platform,
                                               @ModelAttribute SocialMedia socialMediaUpdate) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = influencerService.findSocialMediaByPlatform(influencer, platform);

        // Update social media details
        socialMedia.setAccountUrl(socialMediaUpdate.getAccountUrl());
        socialMedia.setFollowers(socialMediaUpdate.getFollowers());
        socialMedia.setUsername(socialMediaUpdate.getUsername());
        socialMedia.setProfileViews(socialMediaUpdate.getProfileViews());
        // averageComments and averageLikes are auto-calculated from posts

        // Update influencer's total followers
        influencer.updateTotalFollowers();
        influencerRepository.save(influencer);

        return "redirect:/influencer/social/{platform}";
    }
}
