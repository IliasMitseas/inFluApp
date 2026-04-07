package org.ilias.influapp.services.impl;

import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.entities.Reaction;
import org.ilias.influapp.services.EmojiSentimentAnalyzer;
import org.springframework.stereotype.Service;

import java.util.*;

// A simple rule-based sentiment analyzer that combines reaction types and emoji usage in comments.
@Service
public class EmojisEmojiSentimentAnalyzerImpl implements EmojiSentimentAnalyzer {

    // ── Emoji dictionaries ──
    private static final Set<String> POSITIVE_EMOJIS = Set.of(
            "❤️", "😍", "🥰", "😊", "😁", "👍", "👏", "🙌", "💯", "🔥",
            "⭐", "✨", "💪", "🎉", "🤩", "😃", "🥳", "💖", "💕", "🌟"
    );

    private static final Set<String> NEGATIVE_EMOJIS = Set.of(
            "😡", "😠", "👎", "💩", "🤮", "😤", "😒", "😞", "😢", "😭",
            "💔", "🙄", "😑", "🤬", "👹"
    );

    @Override
    public PostSentiment analyzeEmojiSentiment(List<Reaction> reactions, List<String> comments) {
        if ((reactions == null || reactions.isEmpty()) && (comments == null || comments.isEmpty())) {
            return PostSentiment.NEUTRAL;
        }

        double reactionScore = calculateReactionScore(reactions);
        double emojiScore = calculateEmojiScore(comments);

        // If we have both, weighted average (reactions weigh more)
        double finalScore;
        if (reactions != null && !reactions.isEmpty() && comments != null && !comments.isEmpty()) {
            finalScore = reactionScore * 0.7 + emojiScore * 0.3;
        } else if (reactions != null && !reactions.isEmpty()) {
            finalScore = reactionScore;
        } else {
            finalScore = emojiScore;
        }

        return scoreToSentiment(finalScore);
    }

    @Override
    public double calculateReactionScore(List<Reaction> reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return 0.0;
        }

        int totalReactions = 0;
        double weightedSum = 0.0;

        for (Reaction reaction : reactions) {
            int count = reaction.getCount();
            totalReactions += count;

            double weight = reactionWeight(reaction);
            weightedSum += count * weight;
        }

        if (totalReactions == 0) {
            return 0.0;
        }

        // Normalize to [-1, 1] and apply smoothing curve
        double normalized = weightedSum / totalReactions;
        return Math.tanh(normalized * 1.2);
    }

    private double reactionWeight(Reaction reaction) {
        if (reaction == null || reaction.getType() == null) {
            return 0.0;
        }

        String type = reaction.getType().name();
        return switch (type) {
            case "LOVE" -> 0.8;
            case "LIKE" -> 0.6;
            case "WOW", "HAHA", "NEUTRAL" -> 0.2;
            case "SAD", "ANGRY", "DISLIKE", "TERRIBLE" -> -0.7;
            default -> 0.0;
        };
    }

    @Override
    public double calculateEmojiScore(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        int count = 0;
        for (String comment : comments) {
            double score = analyzeEmojis(comment);
            if (score != 0.0) {
                total += score;
                count++;
            }
        }
        return count == 0 ? 0.0 : total / count;
    }


    private double analyzeEmojis(String comment) {
        double emojiScore = 0.0;

        for (String emoji : POSITIVE_EMOJIS) {
            if (comment.contains(emoji)) {
                emojiScore += 0.3;
            }
        }

        for (String emoji : NEGATIVE_EMOJIS) {
            if (comment.contains(emoji)) {
                emojiScore -= 0.3;
            }
        }
        return Math.max(-0.5, Math.min(0.5, emojiScore));
    }

    private PostSentiment scoreToSentiment(double score) {
        if (score >= 0.55) {
            return PostSentiment.LOVE;
        } else if (score >= 0.15) {
            return PostSentiment.LIKE;
        } else if (score >= -0.15) {
            return PostSentiment.NEUTRAL;
        } else if (score >= -0.55) {
            return PostSentiment.DISLIKE;
        } else {
            return PostSentiment.TERRIBLE;
        }
    }
}
