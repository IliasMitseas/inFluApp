package org.ilias.influapp.services;

import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.CountsRequest;
import org.ilias.influapp.entities.Post;
import org.ilias.influapp.entities.PostSentiment;
import org.ilias.influapp.entities.Reaction;
import org.ilias.influapp.entities.SocialMedia;

import java.util.List;

public interface PostService {
    Post createPostFromDto(PostDto postDto, SocialMedia socialMedia);

    List<String> parseCommentsFromText(String commentsText);

    List<Reaction> createReactionsFromCounts(Post post, CountsRequest countsRequest);

    PostSentiment calculateAutoSentiment(List<Reaction> reactions, List<String> comments);
}
