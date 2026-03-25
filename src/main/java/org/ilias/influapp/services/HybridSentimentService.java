package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.entities.Post;
import org.ilias.influapp.entities.Reaction;
import org.ilias.influapp.entities.SentimentAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HybridSentimentService {

    private static final Logger log = LoggerFactory.getLogger(HybridSentimentService.class);

    private final CoreNlpSentimentService coreNlpService;
    private final EmojiSentimentAnalyzer emojiSentimentAnalyzer;


    public PostSentiment analyzeAndSave(Post post, List<Reaction> reactions, List<String> comments) {

        boolean hasText = (comments != null && !comments.isEmpty()) || (post.getContent() != null && !post.getContent().isBlank());

        boolean hasReactions = reactions != null && !reactions.isEmpty();

        // 1. CoreNLP — analyze text
        SentimentResult nlpResult = analyzeText(post, comments);
        double coreNlpScore = nlpResult.polarity();
        double confidence = nlpResult.confidence();

        // 2. Emoji — analyze emoji in comments
        double emojiScore = emojiSentimentAnalyzer.calculateEmojiScore(comments);

        // 3. Reactions
        double reactionScore = emojiSentimentAnalyzer.calculateReactionScore(reactions);

        // Weighted average — adapt weights dynamically using signal strengths
        double finalPolarity;
        String methodDesc;

        if (!hasText && !hasReactions) {
            finalPolarity = 0.0;
            confidence = 0.5;
            methodDesc = "No data — defaulted to Neutral";
        } else if (!hasText) {
            // Only reactions available
            finalPolarity = reactionScore;
            confidence = 0.5;
            methodDesc = "Reactions only (100%)";
        } else if (!hasReactions) {
            // Only text available
            finalPolarity = coreNlpScore * 0.80 + emojiScore * 0.20;
            methodDesc = "CoreNLP (80%) + Emoji (20%)";
        } else {
            // Both text and reactions present — compute dynamic weights
            int totalReactionsCount = 0;
            if (reactions != null) {
                for (Reaction r : reactions) {
                    if (r != null && r.getCount() != null) totalReactionsCount += r.getCount();
                }
            }

            double absReaction = Math.abs(reactionScore);

            // Base weights
            double wCore = 0.70;
            double wEmoji = 0.15;
            double wReaction = 0.15;

            // If CoreNLP shows low confidence (e.g. non-English text) reduce its weight
            if (confidence < 0.6) {
                // scale down core weight proportionally but keep a minimum
                double factor = Math.max(0.4, confidence); // don't reduce below 0.4
                wCore *= factor;
                // redistribute freed weight to reactions
                wReaction += (0.70 - wCore) * 0.6;
                wEmoji += (0.70 - wCore) * 0.4;
            }

            // If reactions are both numerous and strongly negative/positive, favor them
            if (totalReactionsCount >= 200 && absReaction > 0.5) {
                wReaction = Math.max(wReaction, 0.60);
                // reduce core proportionally
                wCore = Math.max(0.20, wCore - 0.30);
                methodDesc = "Reactions-dominated (high volume & strong polarity)";
            } else if (totalReactionsCount >= 100 && absReaction > 0.4) {
                wReaction = Math.max(wReaction, 0.45);
                wCore = Math.max(0.25, wCore - 0.15);
                methodDesc = "Reactions-strong (moderate volume)";
            } else {
                methodDesc = "CoreNLP + Emoji + Reactions (adaptive)";
            }

            // Normalize weights to sum to 1
            double sum = wCore + wEmoji + wReaction;
            wCore /= sum; wEmoji /= sum; wReaction /= sum;

            finalPolarity = coreNlpScore * wCore + emojiScore * wEmoji + reactionScore * wReaction;
        }

        // Clamp to [-1, 1]
        finalPolarity = Math.max(-1.0, Math.min(1.0, finalPolarity));

        PostSentiment sentiment = polarityToSentiment(finalPolarity);

        // Build and attach SentimentAnalysis (cascade-saved with Post)
        SentimentAnalysis analysis = SentimentAnalysis.builder()
                .post(post)
                .polarity(round3(finalPolarity))
                .label(sentimentToLabel(sentiment))
                .confidence(round3(confidence))
                .modelVersion("Hybrid-v1.0 (" + CoreNlpSentimentService.MODEL_VERSION + ")")
                .method(methodDesc)
                .analyzedAt(LocalDateTime.now())
                .coreNlpScore(round3(coreNlpScore))
                .emojiScore(round3(emojiScore))
                .reactionScore(round3(reactionScore))
                .build();

        post.setSentimentAnalysis(analysis);

        log.info("Sentiment for post {}: {} (polarity={}, confidence={})", post.getId(), analysis.getLabel(), analysis.getPolarity(), analysis.getConfidence());

        return sentiment;
    }


    private SentimentResult analyzeText(Post post, List<String> comments) {
        boolean hasComments = comments != null && !comments.isEmpty();
        boolean hasContent = post.getContent() != null && !post.getContent().isBlank();

        if (!hasComments && !hasContent) {
            return new SentimentResult(0.0, "Neutral", 0.5, 2);
        }

        // Only comments
        if (hasComments && !hasContent) {
            return coreNlpService.analyzeComments(comments);
        }

        // Only content
        if (!hasComments) {
            return coreNlpService.analyze(post.getContent());
        }


        SentimentResult fromComments = coreNlpService.analyzeComments(comments);
        SentimentResult fromContent = coreNlpService.analyze(post.getContent());
        return new SentimentResult(
                fromComments.polarity() * 0.8 + fromContent.polarity() * 0.2,
                fromComments.label(),
                fromComments.confidence() * 0.8 + fromContent.confidence() * 0.2,
                fromComments.sentimentClass()
        );
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private PostSentiment polarityToSentiment(double polarity) {
        if (polarity >= 0.4) return PostSentiment.LOVE;
        if (polarity >= 0.1) return PostSentiment.LIKE;
        if (polarity >= -0.1) return PostSentiment.NEUTRAL;
        if (polarity >= -0.4) return PostSentiment.DISLIKE;
        return PostSentiment.TERRIBLE;
    }

    private String sentimentToLabel(PostSentiment sentiment) {
        return switch (sentiment) {
            case LOVE -> "Very Positive";
            case LIKE -> "Positive";
            case NEUTRAL -> "Neutral";
            case DISLIKE -> "Negative";
            case TERRIBLE -> "Very Negative";
        };
    }
}

