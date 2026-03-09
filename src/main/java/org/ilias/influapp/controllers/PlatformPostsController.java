package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.entities.Enums.Platform;
import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.PostRepository;
import org.ilias.influapp.services.InfluencerService;
import org.ilias.influapp.services.PostService;
import org.ilias.influapp.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PlatformPostsController {

    private final UserService userService;
    private final InfluencerRepository influencerRepository;
    private final InfluencerService influencerService;
    private final PostRepository postRepository;
    private final PostService postService;

    @GetMapping("/influencer/social/{platform}/posts")
    public String viewInfluencerPosts(Authentication authentication, @PathVariable Platform platform, Model model) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = influencerService.findSocialMediaByPlatform(influencer, platform);
        List<Post> posts = postRepository.findBySocialMediaId(socialMedia.getId());

        model.addAttribute("influencer", influencer);
        model.addAttribute("platform", platform);
        model.addAttribute("socialMedia", socialMedia);
        model.addAttribute("posts", posts);
        model.addAttribute("postDto", new PostDto());

        return "influencer-posts";
    }

    @PostMapping("/influencer/social/{platform}/posts/add")
    public String addInfluencerPost(Authentication authentication,
                                    @PathVariable Platform platform,
                                    @ModelAttribute PostDto postDto,
                                    @RequestParam(required = false) String commentsText,
                                    @ModelAttribute CountsRequest countsRequest) {

        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = influencerService.findSocialMediaByPlatform(influencer, platform);

        // Parse comments
        List<String> comments = postService.parseCommentsFromText(commentsText);
        postDto.setComments(comments);

        // Create post
        Post post = postService.createPostFromDto(postDto, socialMedia);

        // Create and set reactions
        List<Reaction> reactions = postService.createReactionsFromCounts(post, countsRequest);
        post.setReactions(reactions);

        // Calculate sentiment and engagement rate
        PostSentiment autoSentiment = postService.calculateAndSaveHybridSentiment(post, reactions, comments);
        post.setPostSentiment(autoSentiment);
        post.calculateAndSetEngagementRate();

        // Add post to social media using proper helper method
        socialMedia.addPost(post);
        postRepository.save(post);

        // Update influencer overall engagement rate
        influencer.updateEngagementRate();
        influencer.updateInfluencerScore();
        influencerRepository.save(influencer);

        return "redirect:/influencer/social/" + platform + "/posts";
    }

    @PostMapping("/influencer/social/{platform}/posts/{postId}/delete")
    public String deletePost(Authentication authentication,
                             @PathVariable Platform platform,
                             @PathVariable Long postId) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);

        // Remove from collaboration if linked
        if (post.getCollaboration() != null) {
            post.getCollaboration().getPosts().remove(post);
        }

        // Remove from social media (orphanRemoval will delete the post)
        SocialMedia socialMedia = influencerService.findSocialMediaByPlatform(influencer, platform);
        socialMedia.getPosts().remove(post);

        // Update influencer engagement rate
        influencer.updateEngagementRate();
        influencer.updateInfluencerScore();
        influencerRepository.save(influencer);

        return "redirect:/influencer/social/" + platform + "/posts";
    }
}
