package org.ilias.influapp.services;

import org.ilias.influapp.dtos.ProfilePlatformsForm;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface InfluencerService {
    ProfilePlatformsForm getProfilePlatformsForm(Long influencerId);
    Influencer updateInfluencer(Influencer updateInfluencer, User currentUser);
    void updateInfluencerPlatforms(Long influencerId, ProfilePlatformsForm platformsForm);
    void uploadProfileImage(Long influencerId, MultipartFile file) throws IOException;

}
