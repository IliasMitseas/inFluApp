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
import java.util.Set;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HybridSentimentService {

    private static final Logger log = LoggerFactory.getLogger(HybridSentimentService.class);

    private final CoreNlpSentimentService coreNlpService;
    private final EmojiSentimentAnalyzer emojiSentimentAnalyzer;

    // Small Greek lexicon to help when CoreNLP confidence is low or text is Greek
    private static final Set<String> GREEK_NEGATIVE_KEYWORDS = Set.of(
            "απαίσ", "χάλια", "απαράδ", "φρικτ", "απαίσιο", "απαράδεκτο", "δεν μου αρέσε", "δεν το προτείνω", "σπασμενο", "απάτη"
    );
    private static final Set<String> GREEK_POSITIVE_KEYWORDS = Set.of(
            "λάτρ", "λατρεψ", "τέλει", "τέλεια", "μπράβο", "υπέροχ", "σούπερ", "το λάτρεψα"
    );


    public PostSentiment analyzeAndSave(Post post, List<Reaction> reactions, List<String> comments) {

        boolean hasText = (comments != null && !comments.isEmpty()) || (post.getContent() != null && !post.getContent().isBlank());

        boolean hasReactions = reactions != null && !reactions.isEmpty();

        // 1. CoreNLP — analyze text
        SentimentResult nlpResult = analyzeText(post, comments);
        double coreNlpScore = nlpResult.polarity();
        double confidence = nlpResult.confidence();

        // Apply Greek lexicon bias when appropriate (helps where CoreNLP struggles with Greek)
        double lexiconBias = detectGreekLexiconBias(comments, post.getContent());
        if (lexiconBias != 0.0) {
            // If CoreNLP confidence is low (<0.7), lean more on lexicon; otherwise blend
            double lexWeight = confidence < 0.7 ? 0.7 : 0.35;
            coreNlpScore = clamp(coreNlpScore * (1.0 - lexWeight) + lexiconBias * lexWeight);
            // slightly reduce confidence when applying lexicon correction
            confidence = Math.min(1.0, confidence + Math.abs(lexiconBias) * 0.1);
            log.debug("Applied lexicon bias {} to coreNlpScore -> {} (conf={}) for post {}", lexiconBias, coreNlpScore, confidence, post.getId());
        }

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
            finalPolarity = coreNlpScore * 0.85 + emojiScore * 0.15;
            methodDesc = "CoreNLP (85%) + Emoji (15%)";
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
                double factor = Math.max(0.35, confidence); // allow slightly lower floor
                wCore *= factor;
                // redistribute freed weight to reactions and emoji
                wReaction += (0.70 - wCore) * 0.7;
                wEmoji += (0.70 - wCore) * 0.3;
            }

            // If reactions are both numerous and strongly negative/positive, favor them
            if (totalReactionsCount >= 200 && absReaction > 0.55) {
                // Give reactions much stronger influence for high-volume, high-polarity signals
                wReaction = Math.max(wReaction, 0.85);
                // reduce core and emoji proportionally
                wCore = Math.max(0.08, wCore * 0.15);
                wEmoji = Math.max(0.03, wEmoji * 0.3);
                methodDesc = "Reactions-dominated (high volume & strong polarity)";
            } else if (totalReactionsCount >= 120 && absReaction > 0.45) {
                wReaction = Math.max(wReaction, 0.55);
                wCore = Math.max(0.22, wCore - 0.18);
                methodDesc = "Reactions-strong (moderate volume)";
            } else {
                methodDesc = "CoreNLP + Emoji + Reactions (adaptive)";
            }

            // Normalize weights to sum to 1
            double sum = wCore + wEmoji + wReaction;
            wCore /= sum; wEmoji /= sum; wReaction /= sum;

            finalPolarity = coreNlpScore * wCore + emojiScore * wEmoji + reactionScore * wReaction;

            // If reaction signal is extremely strong (very negative/very positive) favor it directly
            if (absReaction > 0.75 && totalReactionsCount >= 100) {
                finalPolarity = reactionScore * 0.98 + finalPolarity * 0.02; // almost pure reaction
                methodDesc += " [reaction override]";
            }
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

        log.info("Sentiment for post {}: {} (polarity={}, confidence={}, method={})", post.getId(), analysis.getLabel(), analysis.getPolarity(), analysis.getConfidence(), methodDesc);

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
        // Adjusted thresholds to better match manual labels and reaction overrides
        if (polarity >= 0.40) return PostSentiment.LOVE;
        if (polarity >= 0.12) return PostSentiment.LIKE;
        if (polarity >= -0.05) return PostSentiment.NEUTRAL;
        if (polarity >= -0.30) return PostSentiment.DISLIKE;
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

    private double clamp(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(-1.0, Math.min(1.0, v));
    }

    private double detectGreekLexiconBias(List<String> comments, String content) {
        double bias = 0.0;
        if (content != null && !content.isBlank()) {
            String lower = content.toLowerCase(Locale.ROOT);
            for (String k : GREEK_NEGATIVE_KEYWORDS) if (lower.contains(k)) { bias -= 0.8; break; }
            for (String k : GREEK_POSITIVE_KEYWORDS) if (lower.contains(k)) { bias += 0.8; break; }
        }
        if (comments != null && !comments.isEmpty()) {
            for (String c : comments) {
                if (c == null) continue;
                String lower = c.toLowerCase(Locale.ROOT);
                for (String k : GREEK_NEGATIVE_KEYWORDS) if (lower.contains(k)) { bias -= 0.8; break; }
                for (String k : GREEK_POSITIVE_KEYWORDS) if (lower.contains(k)) { bias += 0.8; break; }
                if (bias != 0.0) break;
            }
        }
        // clamp bias to [-1,1]
        return clamp(bias);
    }
}
