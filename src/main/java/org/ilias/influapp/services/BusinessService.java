package org.ilias.influapp.services;

import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BusinessService {

    Business updateBusinessProfile(User user, Business updatedBusiness);

    void uploadProfileImage(Long businessId, MultipartFile file) throws IOException;
}
