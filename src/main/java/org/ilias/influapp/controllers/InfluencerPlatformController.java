package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.Platform;
import org.ilias.influapp.entities.SocialMedia;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
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
    private final UserService userService;

    @GetMapping("/influencer/social/{platform}")
    public String influencerSocialPlatform(Authentication authentication, @PathVariable Platform platform, Model model) {
        User user = userService.currentUser(authentication);
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



    @PostMapping("/influencer/social/{platform}/edit")
    public String editInfluencerSocialPlatform(Authentication authentication,
                                               @PathVariable Platform platform,
                                               @ModelAttribute SocialMedia socialMediaUpdate) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        SocialMedia socialMedia = null;
        if (influencer.getSocialMediaAccounts() != null) {
            socialMedia = influencer.getSocialMediaAccounts().stream()
                    .filter(sm -> sm != null && platform.equals(sm.getPlatform()))
                    .findFirst()
                    .orElse(null);
        }
        if (socialMedia != null) {
            socialMedia.setAccountUrl(socialMediaUpdate.getAccountUrl());
            socialMedia.setFollowers(socialMediaUpdate.getFollowers());
            socialMedia.setUsername(socialMediaUpdate.getUsername());
            socialMedia.setAverageComments(socialMediaUpdate.getAverageComments());
            socialMedia.setProfileViews(socialMediaUpdate.getProfileViews());
            socialMedia.setAverageLikes(socialMediaUpdate.getAverageLikes());
            influencer.updateTotalFollowers();
            influencerRepository.save(influencer);
        }
        return "redirect:/influencer/home";
    }
}
