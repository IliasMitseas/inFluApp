package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.CampaignStatus;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.CampaignRepository;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/business/campaigns")
public class CampaignController {

    private final CampaignRepository campaignRepository;
    private final BusinessRepository businessRepository;
    private final UserService userService;

    @GetMapping
    public String listCampaigns(Authentication authentication, Model model) {
        User current = userService.currentUser(authentication);
        Business business = businessRepository.findById(current.getId()).orElseThrow(NotFoundException::new);

        List<Campaign> campaigns = campaignRepository.findByBusinessId(business.getId());
        model.addAttribute("campaigns", campaigns);
        return "business-campaigns";
    }

    @GetMapping("/new")
    public String newCampaignForm(Model model) {
        model.addAttribute("campaign", new Campaign());
        model.addAttribute("categories", Category.values());
        model.addAttribute("statuses", CampaignStatus.values());
        model.addAttribute("isNew", true);
        return "campaign-form";
    }

    @PostMapping("/new")
    @Transactional
    public String createCampaign(Authentication authentication,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam Category targetCategory,
                                 @RequestParam Double budget,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) String goals) {
        User current = userService.currentUser(authentication);
        Business business = businessRepository.findById(current.getId()).orElseThrow(NotFoundException::new);

        Campaign campaign = Campaign.builder()
                .business(business)
                .title(title)
                .description(description)
                .targetCategory(targetCategory)
                .budget(budget)
                .status(CampaignStatus.DRAFT)
                .goals(goals)
                .build();

        if (startDate != null && !startDate.isBlank()) {
            campaign.setStartDate(java.time.LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            campaign.setEndDate(java.time.LocalDate.parse(endDate));
        }

        campaignRepository.save(campaign);
        return "redirect:/business/campaigns";
    }

    @GetMapping("/{id}")
    public String viewCampaign(@PathVariable Long id, Authentication authentication, Model model) {
        User current = userService.currentUser(authentication);
        Campaign campaign = campaignRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!campaign.getBusiness().getId().equals(current.getId())) {
            return "redirect:/business/campaigns?error=forbidden";
        }

        model.addAttribute("campaign", campaign);
        return "campaign-view";
    }

    @GetMapping("/{id}/edit")
    public String editCampaignForm(@PathVariable Long id, Authentication authentication, Model model) {
        User current = userService.currentUser(authentication);
        Campaign campaign = campaignRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!campaign.getBusiness().getId().equals(current.getId())) {
            return "redirect:/business/campaigns?error=forbidden";
        }

        model.addAttribute("campaign", campaign);
        model.addAttribute("categories", Category.values());
        model.addAttribute("statuses", CampaignStatus.values());
        model.addAttribute("isNew", false);
        return "campaign-form";
    }

    @PostMapping("/{id}/edit")
    @Transactional
    public String updateCampaign(@PathVariable Long id,
                                 Authentication authentication,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam Category targetCategory,
                                 @RequestParam CampaignStatus status,
                                 @RequestParam Double budget,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) String goals) {
        User current = userService.currentUser(authentication);
        Campaign campaign = campaignRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!campaign.getBusiness().getId().equals(current.getId())) {
            return "redirect:/business/campaigns?error=forbidden";
        }

        campaign.setTitle(title);
        campaign.setDescription(description);
        campaign.setTargetCategory(targetCategory);
        campaign.setStatus(status);
        campaign.setBudget(budget);
        campaign.setGoals(goals);

        if (startDate != null && !startDate.isBlank()) {
            campaign.setStartDate(java.time.LocalDate.parse(startDate));
        } else {
            campaign.setStartDate(null);
        }
        if (endDate != null && !endDate.isBlank()) {
            campaign.setEndDate(java.time.LocalDate.parse(endDate));
        } else {
            campaign.setEndDate(null);
        }

        campaignRepository.save(campaign);
        return "redirect:/business/campaigns/" + id;
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteCampaign(@PathVariable Long id, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Campaign campaign = campaignRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!campaign.getBusiness().getId().equals(current.getId())) {
            return "redirect:/business/campaigns?error=forbidden";
        }

        // Remove from business's list
        Business business = campaign.getBusiness();
        business.getCampaigns().remove(campaign);

        campaignRepository.delete(campaign);
        return "redirect:/business/campaigns";
    }
}

