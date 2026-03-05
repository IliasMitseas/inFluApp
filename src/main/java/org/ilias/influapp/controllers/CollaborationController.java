package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Collaboration;
import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.ilias.influapp.entities.User;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.CollaborationRepository;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationRepository collaborationRepository;
    private final UserService userService;

    @GetMapping("/collaborations/{id}")
    public String viewCollaboration(@PathVariable Long id, Authentication authentication, Model model) {
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        // Ensure the current user is either the influencer or the business that owns the campaign
        User current = userService.currentUser(authentication);
        boolean allowed = false;
        if (current != null) {
            if (collab.getInfluencer() != null && collab.getInfluencer().getId().equals(current.getId())) {
                allowed = true;
            }
            if (!allowed && collab.getCampaign() != null && collab.getCampaign().getBusiness() != null) {
                Business b = collab.getCampaign().getBusiness();
                if (b.getId().equals(current.getId())) {
                    allowed = true;
                }
            }
        }

        if (!allowed) {
            return "redirect:/error?forbidden";
        }

        model.addAttribute("collaboration", collab);
        return "collaboration-view"; // create view template or reuse existing if present
    }

    @PostMapping("/collaborations/{id}/accept")
    @Transactional
    public String acceptCollaboration(@PathVariable Long id, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        // Only the influencer who received the request should accept
        if (!collab.getInfluencer().getId().equals(current.getId())) {
            return "redirect:/influencer/home?error=forbidden";
        }

        collab.setStatus(CollaborationStatus.ACCEPTED);
        collaborationRepository.save(collab);
        return "redirect:/influencer/home?success=collab_accepted";
    }

    @PostMapping("/collaborations/{id}/reject")
    @Transactional
    public String rejectCollaboration(@PathVariable Long id, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        // Only the influencer who received the request should reject
        if (!collab.getInfluencer().getId().equals(current.getId())) {
            return "redirect:/influencer/home?error=forbidden";
        }

        collab.setStatus(CollaborationStatus.REJECTED);
        collaborationRepository.save(collab);
        return "redirect:/influencer/home?success=collab_rejected";
    }
}
