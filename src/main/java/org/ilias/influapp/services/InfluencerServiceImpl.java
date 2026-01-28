package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.ProfilePlatformsForm;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.Platform;
import org.ilias.influapp.entities.SocialMedia;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InfluencerServiceImpl implements InfluencerService {

    private final InfluencerRepository influencerRepository;

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
        return influencer;
    }


    @Transactional
    public void updateInfluencerPlatforms(Long influencerId, ProfilePlatformsForm platformsForm) {
        Influencer influencer = influencerRepository.findById(influencerId)
                .orElseThrow(NotFoundException::new);

        if (influencer.getSocialMediaAccounts() == null) {
            influencer.setSocialMediaAccounts(new ArrayList<>());
        }

        EnumSet<Platform> selected = platformsForm == null || platformsForm.getSelectedPlatforms() == null
                ? EnumSet.noneOf(Platform.class) : EnumSet.copyOf(platformsForm.getSelectedPlatforms());

        // Remove unselected platforms
        influencer.getSocialMediaAccounts().removeIf(sm ->
            sm != null && sm.getPlatform() != null && !selected.contains(sm.getPlatform()));

        // Get current platforms after removal
        List<Platform> currentPlatforms = influencer.getSocialMediaAccounts().stream()
                .filter(sm -> sm != null && sm.getPlatform() != null)
                .map(SocialMedia::getPlatform)
                .toList();

        // Add new platforms
        for (Platform platform : selected) {
            if (!currentPlatforms.contains(platform)) {
                SocialMedia sm = new SocialMedia();
                sm.setInfluencer(influencer);
                sm.setPlatform(platform);
                sm.setAccountUrl("https://pending-setup.example.com/" + platform.name().toLowerCase());
                sm.setFollowers(0);
                influencer.getSocialMediaAccounts().add(sm);
            }
        }

        // Recalculate total followers after updating platforms
        influencer.updateTotalFollowers();

        influencerRepository.save(influencer);
    }


    public void uploadProfileImage(Long influencerId, MultipartFile file) throws IOException {
        Influencer influencer = influencerRepository.findById(influencerId).orElseThrow(NotFoundException::new);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Generate safe filename
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String safe = original.replaceAll("[^a-zA-Z0-9.\\-_/]", "_");
        String filename = "influencer-" + influencer.getId() + "-" + System.currentTimeMillis() + "-" + safe;

        // Save file
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(filename).normalize();

        // Security check: prevent directory traversal
        if (!target.startsWith(uploadDir)) {
            throw new SecurityException("Invalid file path");
        }

        file.transferTo(target.toFile());
        influencer.setImageUrl("/uploads/" + filename);
        influencerRepository.save(influencer);
    }
}
