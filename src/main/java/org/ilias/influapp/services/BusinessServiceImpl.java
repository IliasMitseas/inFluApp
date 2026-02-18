package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.BusinessRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepository businessRepository;
    private final ImageUploadService imageUploadService;

    public Business updateBusinessProfile(User user, Business updatedBusiness) {
        Business business = businessRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        business.setCompanyName(updatedBusiness.getCompanyName());
        business.setDescription(updatedBusiness.getDescription());
        business.setWebSite(updatedBusiness.getWebSite());
        business.setCategory(updatedBusiness.getCategory());
        business.setAddress(updatedBusiness.getAddress());
        business.setPhone(updatedBusiness.getPhone());
        businessRepository.save(business);
        return business;
    }

    public void uploadProfileImage(Long businessId, MultipartFile file) throws IOException {
        Business business = businessRepository.findById(businessId).orElseThrow(NotFoundException::new);

        // Use ImageUploadService for upload logic
        String imageUrl = imageUploadService.uploadImage(file, "business", businessId);

        business.setImageUrl(imageUrl);
        businessRepository.save(business);
    }
}
