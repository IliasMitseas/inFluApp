package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.PostDto;
import org.ilias.influapp.entities.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final SentimentAnalyzer sentimentAnalyzer;

    @Override
    public Post createPostFromDto(PostDto postDto, SocialMedia socialMedia) {
        return Post.builder()
                .content(postDto.getContent())
                .reactions(postDto.getReactions())
                .comments(postDto.getComments())
                .shares(postDto.getShares())
                .reach(postDto.getReach())
                .impressionCount(postDto.getImpressionCount())
                .socialMedia(socialMedia)
                .build();
    }

    @Override
    public List<String> parseCommentsFromText(String commentsText) {
        List<String> commentsList = new ArrayList<>();

        if (commentsText != null && !commentsText.trim().isEmpty()) {
            String[] commentLines = commentsText.split("\\r?\\n");
            for (String line : commentLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    commentsList.add(trimmed);
                }
            }
        }

        return commentsList;
    }

    @Override
    public List<Reaction> createReactionsFromCounts(Post post, CountsRequest countsRequest) {
        List<Reaction> reactions = new ArrayList<>();

        if (countsRequest.getLikeCount() != null && countsRequest.getLikeCount() > 0) {
            reactions.add(createReaction(post, ReactionType.LIKE, countsRequest.getLikeCount()));
        }
        if (countsRequest.getLoveCount() != null && countsRequest.getLoveCount() > 0) {
            reactions.add(createReaction(post, ReactionType.LOVE, countsRequest.getLoveCount()));
        }
        if (countsRequest.getHahaCount() != null && countsRequest.getHahaCount() > 0) {
            reactions.add(createReaction(post, ReactionType.HAHA, countsRequest.getHahaCount()));
        }
        if (countsRequest.getWowCount() != null && countsRequest.getWowCount() > 0) {
            reactions.add(createReaction(post, ReactionType.WOW, countsRequest.getWowCount()));
        }
        if (countsRequest.getSadCount() != null && countsRequest.getSadCount() > 0) {
            reactions.add(createReaction(post, ReactionType.SAD, countsRequest.getSadCount()));
        }
        if (countsRequest.getAngryCount() != null && countsRequest.getAngryCount() > 0) {
            reactions.add(createReaction(post, ReactionType.ANGRY, countsRequest.getAngryCount()));
        }

        return reactions;
    }

    private Reaction createReaction(Post post, ReactionType type, Integer count) {
        Reaction reaction = new Reaction();
        reaction.setPost(post);
        reaction.setType(type);
        reaction.setCount(count);
        return reaction;
    }

    @Override
    public PostSentiment calculateAutoSentiment(List<Reaction> reactions, List<String> comments) {
        return sentimentAnalyzer.analyzeSentiment(reactions, comments);
    }
}
