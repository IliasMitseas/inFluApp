package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.CollaborationRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.PostRepository;
import org.ilias.influapp.repository.SocialMediaRepository;
import org.ilias.influapp.services.PostService;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Arrays;

@Controller
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationRepository collaborationRepository;
    private final InfluencerRepository influencerRepository;
    private final SocialMediaRepository socialMediaRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final UserService userService;

    @GetMapping("/collaborations/{id}")
    public String viewCollaboration(@PathVariable Long id, Authentication authentication, Model model) {
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

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
        return "collaboration-view";
    }



    @GetMapping("/influencer/collaborations/active")
    public String influencerActiveCollaborations(Authentication authentication, Model model) {
        User current = userService.currentUser(authentication);

        if (current == null) {
            return "redirect:/login";
        }

        List<CollaborationStatus> activeStatuses = Arrays.asList(CollaborationStatus.ACCEPTED, CollaborationStatus.IN_PROGRESS);
        List<Collaboration> active = collaborationRepository.findByInfluencerIdAndStatusIn(current.getId(), activeStatuses);

        model.addAttribute("collaborations", active);

        return "influencer-collaborations";
    }

    @PostMapping("/collaborations/{id}/accept")
    @Transactional
    public String acceptCollaboration(@PathVariable Long id, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!collab.getInfluencer().getId().equals(current.getId())) {
            return "redirect:/influencer/home?error=forbidden";
        }

        collab.setStatus(org.ilias.influapp.entities.Enums.CollaborationStatus.ACCEPTED);
        collaborationRepository.save(collab);

        return "redirect:/influencer/home?success=collab_accepted";
    }

    @PostMapping("/collaborations/{id}/reject")
    @Transactional
    public String rejectCollaboration(@PathVariable Long id, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!collab.getInfluencer().getId().equals(current.getId())) {
            return "redirect:/influencer/home?error=forbidden";
        }

        collab.setStatus(org.ilias.influapp.entities.Enums.CollaborationStatus.REJECTED);
        collaborationRepository.save(collab);

        return "redirect:/influencer/home?success=collab_rejected";
    }

    @GetMapping("/collaborations/{id}/posts/new")
    public String createPostForCollab(@PathVariable Long id, Authentication authentication, Model model) {
        User current = userService.currentUser(authentication);

        Influencer influencer = influencerRepository.findById(current.getId()).orElseThrow(NotFoundException::new);

        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        List<SocialMedia> socialMediaAccounts = influencer.getSocialMediaAccounts();

        model.addAttribute("collaboration", collab);
        model.addAttribute("socialMediaAccounts", socialMediaAccounts);

        return "collaboration-post-form";
    }

    @PostMapping("/collaborations/{id}/posts")
    @Transactional
    public String savePostForCollab(Authentication authentication,
                                    @PathVariable Long id,
                                    @RequestParam Long socialMediaId,
                                    @ModelAttribute PostDto postDto,
                                    @RequestParam(required = false) String commentsText,
                                    @ModelAttribute CountsRequest countsRequest) {
        User user = userService.currentUser(authentication);

        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = socialMediaRepository.findById(socialMediaId).orElseThrow(NotFoundException::new);

        // Parse comments
        List<String> comments = postService.parseCommentsFromText(commentsText);
        postDto.setComments(comments);

        // Create post from DTO
        Post post = postService.createPostFromDto(postDto, socialMedia);
        post.setCollaboration(collab);

        // Create and set reactions
        List<Reaction> reactions = postService.createReactionsFromCounts(post, countsRequest);
        post.setReactions(reactions);

        // Calculate sentiment and engagement rate
        PostSentiment autoSentiment = postService.calculateAutoSentiment(reactions, comments);
        post.setPostSentiment(autoSentiment);
        post.calculateAndSetEngagementRate();

        // Add post to collaboration and social media
        collab.addPost(post);
        socialMedia.addPost(post);
        postRepository.save(post);

        // Update influencer's overall engagement rate
        influencer.updateEngagementRate();
        influencerRepository.save(influencer);

        return "redirect:/collaborations/" + collab.getId();
    }
}
