package org.ilias.influapp.services;

import org.ilias.influapp.entities.PostSentiment;
import org.ilias.influapp.entities.Reaction;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SentimentAnalyzerImpl implements SentimentAnalyzer {

    private static final Set<String> VERY_POSITIVE_KEYWORDS = Set.of(
            "love", "amazing", "excellent", "perfect", "wonderful", "fantastic", "brilliant", "incredible", "outstanding",
            "superb", "spectacular", "exceptional", "phenomenal", "magnificent", "masterpiece",
            "τέλειο", "υπέροχο", "καταπληκτικό", "φοβερό", "εξαιρετικό", "θαυμάσιο"
    );

    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "great", "awesome", "good", "nice", "best", "beautiful", "impressive", "cool", "thanks", "thank you", "lovely",
            "interesting", "inspiring", "useful", "helpful", "creative", "genius", "solid", "quality",
            "μπράβο", "ωραίο", "όμορφο", "ευχαριστώ", "μοναδικό", "ευχάριστο", "ενδιαφέρον", "χρήσιμο"
    );

    private static final Set<String> MILD_POSITIVE_KEYWORDS = Set.of(
            "ok", "fine", "decent", "alright", "not bad", "fair", "acceptable",
            "καλό", "εντάξει", "οκ", "όχι άσχημο"
    );


    private static final Set<String> VERY_NEGATIVE_KEYWORDS = Set.of(
            "terrible", "awful", "horrible", "worst", "hate", "pathetic", "disgusting", "trash", "garbage", "disaster",
            "appalling", "atrocious",
            "χάλια", "απαίσιο", "αηδία", "άθλιο", "ντροπή", "σκουπίδι", "καταστροφή", "φρικτό", "απαράδεκτο"
    );

    private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
            "bad", "poor", "disappointing", "useless", "waste", "fail", "boring", "lame", "stupid", "sucks", "crap",
            "wrong", "annoying", "weak",
            "κακό", "άσχημο", "βαρετό", "άχρηστο", "απογοητευτικό"
    );

    private static final Set<String> MILD_NEGATIVE_KEYWORDS = Set.of(
            "meh", "mediocre", "lackluster", "underwhelming", "questionable",
            "μέτριο", "έτσι κι έτσι"
    );


    private static final Set<String> POSITIVE_EMOJIS = Set.of(
            "❤️", "😍", "🥰", "😊", "😁", "👍", "👏", "🙌", "💯", "🔥", "⭐", "✨", "💪", "🎉", "🤩", "😃", "🥳", "💖", "💕", "🌟"
    );

    private static final Set<String> NEGATIVE_EMOJIS = Set.of(
            "😡", "😠", "👎", "💩", "🤮", "😤", "😒", "😞", "😢", "😭", "💔", "🙄", "😑", "🤬", "👹"
    );

    private static final Set<String> NEGATION_WORDS = Set.of(
            "not", "no", "never", "neither", "nobody", "nothing", "none", "nowhere", "don't", "doesn't", "didn't",
            "won't", "wouldn't", "can't", "cannot", "shouldn't",
            "δεν", "δε", "μη", "μην", "όχι", "ποτέ", "καμία", "κανένας"
    );

    // Intensifiers
    private static final Set<String> INTENSIFIERS = Set.of(
            "very", "really", "extremely", "super", "absolutely", "totally", "completely", "highly", "so", "such", "too", "incredibly",
            "πολύ", "πάρα πολύ", "εξαιρετικά", "απίστευτα"
    );

    // Dynamic weighting
    private static final int HIGH_VOLUME_THRESHOLD = 50;
    private static final int LOW_VOLUME_THRESHOLD = 5;

    @Override
    public PostSentiment analyzeSentiment(List<Reaction> reactions, List<String> comments) {
        if ((reactions == null || reactions.isEmpty()) && (comments == null || comments.isEmpty())) {
            return PostSentiment.NEUTRAL;
        }

        double reactionScore = calculateReactionScore(reactions);
        double commentScore = calculateCommentScore(comments);

        // Dynamic weighting
        double reactionWeight = calculateDynamicReactionWeight(reactions, comments);
        double commentWeight = 1.0 - reactionWeight;

        // Weighted average
        double finalScore = (reactionScore * reactionWeight) + (commentScore * commentWeight);

        // score σε PostSentiment
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

            double weight = switch (reaction.getType()) {
                case LOVE -> 1.0;
                case LIKE -> 0.65;
                case WOW -> 0.45;
                case HAHA -> 0.25;
                case SAD -> -0.7;
                case ANGRY -> -1.0;
            };
            weightedSum += count * weight;
        }

        if (totalReactions == 0){
            return 0.0;
        }

        // [-1, 1]
        double normalized = weightedSum/totalReactions;

        return applySmoothingCurve(normalized);
    }

    @Override
    public double calculateCommentScore(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        int scoredComments = 0;

        for (String comment : comments) {
            double commentScore = analyzeCommentWithWeights(comment);
            if (commentScore != 0.0) {
                totalScore += commentScore;
                scoredComments++;
            }
        }

        if (scoredComments == 0) {
            return 0.15;
        }
        return totalScore / scoredComments;
    }


    private double analyzeCommentWithWeights(String comment) {
        String lower = comment.toLowerCase();
        double score = 0.0;

        boolean hasNegation = hasNegation(lower);

        double intensifierMultiplier = getIntensifierMultiplier(lower);

        double keywordScore = findKeywordScore(lower);

        if (hasNegation && keywordScore != 0.0) {
            keywordScore = -keywordScore * 0.8;  // Flip but reduce intensity
        }

        //intensifier
        keywordScore *= intensifierMultiplier;

        score += keywordScore;
        score += analyzeEmojis(comment);
        score += analyzeEnthusiasm(comment, score);

        return Math.max(-1.0, Math.min(1.0, score));
    }

    private boolean hasNegation(String lowerComment) {
        for (String negation : NEGATION_WORDS) {
            if (lowerComment.contains(negation + " ")) {
                return true;
            }
        }
        return false;
    }

    private double getIntensifierMultiplier(String lowerComment) {
        int intensifierCount = 0;
        for (String intensifier : INTENSIFIERS) {
            if (lowerComment.contains(intensifier)) {
                intensifierCount++;
            }
        }

        if (intensifierCount == 0) return 1.0;
        if (intensifierCount == 1) return 1.3;  // 30% boost
        return 1.5;  // 50% boost για πολλά intensifiers
    }

    private double findKeywordScore(String lower) {
        for (String keyword : VERY_POSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return 1.0;
            }
        }

        for (String keyword : POSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return 0.7;
            }
        }

        for (String keyword : MILD_POSITIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return 0.4;
            }
        }

        for (String keyword : VERY_NEGATIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return -1.0;
            }
        }

        for (String keyword : NEGATIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return -0.7;
            }
        }

        for (String keyword : MILD_NEGATIVE_KEYWORDS) {
            if (lower.contains(keyword)) {
                return -0.4;
            }
        }

        return 0.0;  // No keyword
    }


    private double analyzeEnthusiasm(String comment, double baseScore) {
        if (baseScore == 0.0) return 0.0;  // No enthusiasm for neutral

        double enthusiasmBonus = 0.0;

        long exclamationCount = comment.chars().filter(ch -> ch == '!').count();
        if (exclamationCount > 0) {
            enthusiasmBonus += Math.min(0.2, exclamationCount * 0.05);  // Max 0.2 bonus
        }

        if (comment.matches(".*[A-Z]{3,}.*")) {
            enthusiasmBonus += 0.15;
        }
        return baseScore > 0 ? enthusiasmBonus : -enthusiasmBonus;
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


    private double calculateDynamicReactionWeight(List<Reaction> reactions, List<String> comments) {
        int totalReactions = reactions == null ? 0 : reactions.stream()
                .mapToInt(Reaction::getCount)
                .sum();
        int totalComments = comments == null ? 0 : comments.size();

        // Base weights
        double baseReactionWeight = 0.6;

        if (totalReactions > HIGH_VOLUME_THRESHOLD && totalComments < LOW_VOLUME_THRESHOLD) {
            return 0.75;
        } else if (totalComments > HIGH_VOLUME_THRESHOLD && totalReactions < LOW_VOLUME_THRESHOLD) {
            return 0.35;
        } else if (totalReactions > totalComments * 10) {
            return 0.70;
        } else if (totalComments > totalReactions) {
            return 0.45;
        }
        return baseReactionWeight;
    }


    private double applySmoothingCurve(double value) {
        return Math.tanh(value * 1.2);  // [-1, 1]
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
