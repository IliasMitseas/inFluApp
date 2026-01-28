package org.ilias.influapp.services;

import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.Post;
import org.ilias.influapp.entities.SocialMedia;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    public Post createPostFromDto(PostDto postDto, SocialMedia socialMedia) {
        return Post.builder()
                .content(postDto.getContent())
                .likes(postDto.getLikes())
                .comments(postDto.getComments())
                .shares(postDto.getShares())
                .reach(postDto.getReach())
                .impressionCount(postDto.getImpressionCount())
                .postSentiment(postDto.getPostSentiment())
                .socialMedia(socialMedia)
                .build();
    }
}
