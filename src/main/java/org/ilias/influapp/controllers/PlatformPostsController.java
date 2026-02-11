package org.ilias.influapp.controllers;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.*;
import org.ilias.influapp.exceptions.NotFoundException;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.PostRepository;
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
    private final PostRepository postRepository;
    private final PostService postService;

    @GetMapping("/influencer/social/{platform}/posts")
    public String viewInfluencerPosts(Authentication authentication, @PathVariable Platform platform, Model model) {
        User user = userService.currentUser(authentication);
        Influencer influencer = influencerRepository.findById(user.getId()).orElseThrow(NotFoundException::new);

        SocialMedia socialMedia = influencer.getSocialMediaAccounts().stream()
                .filter(sm -> sm != null && platform.equals(sm.getPlatform()))
                .findFirst()
                .orElseThrow(NotFoundException::new);

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

        SocialMedia socialMedia = influencer.getSocialMediaAccounts().stream()
                .filter(sm -> sm != null && platform.equals(sm.getPlatform()))
                .findFirst()
                .orElseThrow(NotFoundException::new);

        // Delegate comment parsing to service
        if (commentsText != null && !commentsText.trim().isEmpty()) {
            List<String> comments = postService.parseCommentsFromText(commentsText);
            postDto.setComments(comments);
        }

        // Create post
        Post post = postService.createPostFromDto(postDto, socialMedia);

        // Create reactions
        List<Reaction> reactions = postService.createReactionsFromCounts(post, countsRequest);
        post.setReactions(reactions);

        post.calculateAndSetEngagementRate();
        postRepository.save(post);
        return "redirect:/influencer/social/" + platform + "/posts";
    }
}
