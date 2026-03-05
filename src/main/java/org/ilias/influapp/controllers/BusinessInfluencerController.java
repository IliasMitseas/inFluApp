package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.CampaignStatus;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.CampaignRepository;
import org.ilias.influapp.repository.CollaborationRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class BusinessInfluencerController {

    private final InfluencerRepository influencerRepository;
    private final CollaborationRepository collaborationRepository;
    private final CampaignRepository campaignRepository;
    private final BusinessRepository businessRepository;
    private final UserService userService;

    @GetMapping("/influencers/{id}")
    public String viewInfluencer(@PathVariable Long id, Model model) {
        Influencer influencer = influencerRepository.findById(id).orElseThrow(NotFoundException::new);
        model.addAttribute("influencer", influencer);

        return "influencer-public-profile";
    }


    @GetMapping(value = "/influencer/profile", params = "id")
    public String redirectLegacyProfile(@RequestParam(value = "id", required = false) Long id) {
        if (id == null) {
            return "forward:/influencer/profile";
        }
        return "redirect:/influencers/" + id;
    }



    @GetMapping("/collaborations/request")
    public String requestForm(Authentication authentication,
                              @RequestParam("influencerId") Long influencerId,
                              @RequestParam(value = "error", required = false) String error,
                              Model model) {

        User current = userService.currentUser(authentication);

        if (!(current instanceof Business business)) {
            return "redirect:/login";
        }

        Influencer influencer = influencerRepository.findById(influencerId).orElseThrow(NotFoundException::new);

        model.addAttribute("influencer", influencer);
        model.addAttribute("business", business);
        model.addAttribute("campaigns", business.getCampaigns());
        model.addAttribute("collaboration", new Collaboration());
        model.addAttribute("error", error);

        return "collaboration-request";
    }



    @PostMapping("/collaborations/request")
    @Transactional
    public String submitRequest(Authentication authentication,
                                @RequestParam("influencerId") Long influencerId,
                                @RequestParam(value = "campaignId", required = false) Long campaignId,
                                @RequestParam(value = "newCampaignTitle", required = false) String newCampaignTitle,
                                @RequestParam(value = "newCampaignBudget", required = false) Double newCampaignBudget,
                                @RequestParam(value = "newCampaignCategory", required = false) String newCampaignCategory,
                                @RequestParam("paymentAmount") Double paymentAmount,
                                @RequestParam(name = "deliverables", required = false) String deliverables) {

        User current = userService.currentUser(authentication);

        if (!(current instanceof Business business)) {
            return "redirect:/login";
        }

        Influencer influencer = influencerRepository.findById(influencerId).orElseThrow(NotFoundException::new);

        Campaign campaign = null;

        if (campaignId != null) {
            campaign = business.getCampaigns().stream()
                    .filter(c -> c.getId().equals(campaignId))
                    .findFirst()
                    .orElse(null);

            if (campaign == null) {
                return "redirect:/collaborations/request?influencerId=" + influencerId + "&error=invalid_campaign";
            }

            // Prevent duplicate active collaboration for same influencer and campaign
            if (campaign.getCollaborations() != null) {
                boolean exists = campaign.getCollaborations().stream()
                        .filter(c -> c.getInfluencer() != null && c.getInfluencer().getId().equals(influencerId))
                        .anyMatch(c -> c.getStatus() != CollaborationStatus.REJECTED
                                && c.getStatus() != CollaborationStatus.CANCELLED);
                if (exists) {
                    return "redirect:/collaborations/request?influencerId=" + influencerId + "&error=duplicate_collaboration";
                }
            }
        } else if (newCampaignTitle != null && !newCampaignTitle.isEmpty()) {

            Campaign newCampaign = Campaign.builder()
                    .business(business)
                    .title(newCampaignTitle)
                    .description("Created from collaboration request")
                    .status(CampaignStatus.DRAFT)
                    .build();
            try {
                Category cat = newCampaignCategory != null ? Category.valueOf(newCampaignCategory) : Category.OTHER;
                newCampaign.setTargetCategory(cat);
            } catch (IllegalArgumentException e) {
                newCampaign.setTargetCategory(Category.OTHER);
            }

            newCampaign.setBudget(newCampaignBudget != null ? newCampaignBudget : (paymentAmount != null ? paymentAmount : 0.0));
            newCampaign.setStartDate(LocalDate.now());
            campaign = campaignRepository.save(newCampaign);
            business.addCampaign(campaign);
            businessRepository.save(business);

        } else {

            Campaign lightweight = Campaign.builder()
                    .business(business)
                    .title("Ad-hoc request from " + business.getCompanyName())
                    .description("Auto-created campaign for a single collaboration request")
                    .status(CampaignStatus.DRAFT)
                    .targetCategory(org.ilias.influapp.entities.Enums.Category.OTHER)
                    .budget(paymentAmount != null ? paymentAmount : 0.0)
                    .startDate(LocalDate.now())
                    .build();

            campaign = campaignRepository.save(lightweight);
            business.addCampaign(campaign);
            businessRepository.save(business);
        }

        Collaboration collab = new Collaboration();
        campaign.addCollaboration(collab);
        influencer.addCollaboration(collab);
        collab.setPaymentAmount(paymentAmount != null ? paymentAmount : 0.0);
        collab.setDeliverables(deliverables);
        collab.setStartDate(LocalDate.now());
        collab.setStatus(CollaborationStatus.PENDING);

        collaborationRepository.save(collab);

        return "redirect:/business/home?success=collaboration_requested";
    }
}
