package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.CompanySize;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.services.BusinessService;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessRepository businessRepository;
    private final UserService userService;
    private final BusinessService businessService;

    @GetMapping("/business/home")
    public String businessHome(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        Business business = businessRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        return "business-home";
    }

    @GetMapping("/business/profile")
    public String businessProfile(Authentication authentication, Model model) {
        User user = userService.currentUser(authentication);
        Business business = businessRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        model.addAttribute("business", business);
        model.addAttribute("categories", Category.values());
        model.addAttribute("companySizes", CompanySize.values());
        return "business-profile";
    }

    @PostMapping("/business/profile")
    public String updateBusinessProfile(Authentication authentication, @ModelAttribute("business") Business updateBusiness, Model model) {
        User user = userService.currentUser(authentication);
        Business business = businessService.updateBusinessProfile(user, updateBusiness);

        model.addAttribute("business", business);
        model.addAttribute("categories", Category.values());
        model.addAttribute("companySizes", CompanySize.values());
        model.addAttribute("success", true);
        return "business-profile";
    }

    @PostMapping("/business/profile/image")
    public String uploadProfileImage(Authentication authentication, @RequestParam("file") MultipartFile file) {
        User user = userService.currentUser(authentication);

        try {
            businessService.uploadProfileImage(user.getId(), file);
        } catch (IllegalArgumentException | SecurityException e) {
            return "redirect:/business/profile?error=" + e.getMessage();
        } catch (IOException e) {
            return "redirect:/business/profile?error=upload_failed";
        }
        return "redirect:/business/profile?success=image_uploaded";
    }
}
