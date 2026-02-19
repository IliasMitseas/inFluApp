package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.ProfilePlatformsForm;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.Enums.Platform;
import org.ilias.influapp.entities.SocialMedia;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InfluencerServiceImpl implements InfluencerService {

    private final InfluencerRepository influencerRepository;
    private final ImageUploadService imageUploadService;

    public ProfilePlatformsForm getProfilePlatformsForm(Long influencerId) {
        Influencer influencer = influencerRepository.findById(influencerId).orElseThrow(NotFoundException::new);

        ProfilePlatformsForm platformsForm = new ProfilePlatformsForm();
        if (influencer.getSocialMediaAccounts() != null) {
            for (SocialMedia sm : influencer.getSocialMediaAccounts()) {
                if (sm != null && sm.getPlatform() != null) {
                    platformsForm.getSelectedPlatforms().add(sm.getPlatform());
                }
            }
        }
        return platformsForm;
    }

    public Influencer updateInfluencer(Influencer updateInfluencer, User currentUser) {
        Influencer influencer = influencerRepository.findById(currentUser.getId()).orElseThrow(NotFoundException::new);
        influencer.setName(updateInfluencer.getName());
        influencer.setUsername(updateInfluencer.getUsername());
        influencer.setAge(updateInfluencer.getAge());
        influencer.setLocation(updateInfluencer.getLocation());
        influencer.setBio(updateInfluencer.getBio());
        influencer.setIsAvailable(updateInfluencer.getIsAvailable());
        influencer.setMinCollaborationBudget(updateInfluencer.getMinCollaborationBudget());
        influencer.setCategory(updateInfluencer.getCategory());
        influencer.setInfluencerType(updateInfluencer.getInfluencerType());
        influencer.updateTotalFollowers();
        influencer.updateEngagementRate();
        return influencer;
    }


    @Transactional
    public void updateInfluencerPlatforms(Long influencerId, ProfilePlatformsForm platformsForm) {
        Influencer influencer = influencerRepository.findById(influencerId).orElseThrow(NotFoundException::new);


        EnumSet<Platform> selected = platformsForm == null || platformsForm.getSelectedPlatforms() == null
                ? EnumSet.noneOf(Platform.class) : EnumSet.copyOf(platformsForm.getSelectedPlatforms());

        // Remove unselected platforms (use removeIf for efficiency with JPA cascade)
        influencer.getSocialMediaAccounts().removeIf(sm ->
            sm != null && sm.getPlatform() != null && !selected.contains(sm.getPlatform()));

        // Get current platforms after removal
        List<Platform> currentPlatforms = influencer.getSocialMediaAccounts().stream()
                .filter(sm -> sm != null && sm.getPlatform() != null)
                .map(SocialMedia::getPlatform)
                .toList();

        // Add new platforms using the proper helper method
        for (Platform platform : selected) {
            if (!currentPlatforms.contains(platform)) {
                SocialMedia sm = new SocialMedia();
                sm.setPlatform(platform);
                sm.setAccountUrl("https://pending-setup.example.com/" + platform.name().toLowerCase());
                sm.setFollowers(0);
                influencer.addSocialMediaAccount(sm);
            }
        }

        // Recalculate total followers after updating platforms
        influencer.updateTotalFollowers();
        influencerRepository.save(influencer);
    }


    public void uploadProfileImage(Long influencerId, MultipartFile file) throws IOException {
        Influencer influencer = influencerRepository.findById(influencerId).orElseThrow(NotFoundException::new);

        // Use ImageUploadService for upload logic
        String imageUrl = imageUploadService.uploadImage(file, "influencer", influencerId);

        influencer.setImageUrl(imageUrl);
        influencerRepository.save(influencer);
    }

    /**
     * Helper method to find a SocialMedia account by platform for a given influencer
     */
    public SocialMedia findSocialMediaByPlatform(Influencer influencer, Platform platform) {
        if (influencer == null || platform == null) {
            throw new IllegalArgumentException("Influencer and platform cannot be null");
        }

        return influencer.getSocialMediaAccounts().stream()
                .filter(sm -> sm != null && platform.equals(sm.getPlatform()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Social media account not found for platform: " + platform));
    }
}
