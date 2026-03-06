package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.CollaborationStatus;
import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.*;
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
        model.addAttribute("isInfluencer", current instanceof Influencer);
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

    @GetMapping("/business/collaborations")
    public String businessCollaborations(Authentication authentication, Model model) {
        User current = userService.currentUser(authentication);
        if (current == null) {
            return "redirect:/login";
        }

        List<Collaboration> all = collaborationRepository.findByCampaignBusinessId(current.getId());

        List<Collaboration> pending = all.stream()
                .filter(c -> c.getStatus() == CollaborationStatus.PENDING).toList();
        List<Collaboration> accepted = all.stream()
                .filter(c -> c.getStatus() == CollaborationStatus.ACCEPTED || c.getStatus() == CollaborationStatus.IN_PROGRESS).toList();
        List<Collaboration> completed = all.stream()
                .filter(c -> c.getStatus() == CollaborationStatus.COMPLETED).toList();
        List<Collaboration> rejected = all.stream()
                .filter(c -> c.getStatus() == CollaborationStatus.REJECTED || c.getStatus() == CollaborationStatus.CANCELLED).toList();

        model.addAttribute("pending", pending);
        model.addAttribute("accepted", accepted);
        model.addAttribute("completed", completed);
        model.addAttribute("rejected", rejected);
        model.addAttribute("total", all.size());

        return "business-collaborations";
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

        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        if (!(current instanceof Influencer) || collab.getInfluencer() == null || !collab.getInfluencer().getId().equals(current.getId())) {
            return "redirect:/error?forbidden";
        }

        Influencer influencer = influencerRepository.findById(current.getId()).orElseThrow(NotFoundException::new);
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

        // Update influencer overall engagement rate and score
        influencer.updateEngagementRate();
        influencer.updateInfluencerScore();
        influencerRepository.save(influencer);

        return "redirect:/collaborations/" + collab.getId();
    }

    @PostMapping("/collaborations/{id}/delete")
    @Transactional
    public String deleteCollaboration(@PathVariable Long id, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Collaboration collab = collaborationRepository.findById(id).orElseThrow(NotFoundException::new);

        // Allow delete by the influencer or the owning business
        boolean allowed = false;
        if (collab.getInfluencer() != null && collab.getInfluencer().getId().equals(current.getId())) {
            allowed = true;
        }
        if (!allowed && collab.getCampaign() != null && collab.getCampaign().getBusiness() != null
                && collab.getCampaign().getBusiness().getId().equals(current.getId())) {
            allowed = true;
        }
        if (!allowed) {
            return "redirect:/influencer/home?error=forbidden";
        }

        // Remove from parent collections to avoid orphan conflicts
        if (collab.getCampaign() != null) {
            collab.getCampaign().getCollaborations().remove(collab);
        }
        if (collab.getInfluencer() != null) {
            collab.getInfluencer().getCollaborations().remove(collab);
        }

        collaborationRepository.delete(collab);

        if (current instanceof Influencer) {
            return "redirect:/influencer/collaborations/active";
        }
        return "redirect:/business/collaborations";
    }

    @PostMapping("/collaborations/{collabId}/posts/{postId}/delete")
    @Transactional
    public String deletePost(@PathVariable Long collabId, @PathVariable Long postId, Authentication authentication) {
        User current = userService.currentUser(authentication);
        Collaboration collab = collaborationRepository.findById(collabId).orElseThrow(NotFoundException::new);
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);

        // Only the influencer who owns the collaboration can delete posts
        if (!collab.getInfluencer().getId().equals(current.getId())) {
            return "redirect:/collaborations/" + collabId + "?error=forbidden";
        }

        // Remove from both parent collections — orphanRemoval handles the actual delete
        collab.getPosts().remove(post);
        if (post.getSocialMedia() != null) {
            post.getSocialMedia().getPosts().remove(post);
        }

        // Update influencer engagement rate and score
        Influencer influencer = influencerRepository.findById(current.getId()).orElseThrow(NotFoundException::new);
        influencer.updateEngagementRate();
        influencer.updateInfluencerScore();
        influencerRepository.save(influencer);

        return "redirect:/collaborations/" + collabId;
    }
}
