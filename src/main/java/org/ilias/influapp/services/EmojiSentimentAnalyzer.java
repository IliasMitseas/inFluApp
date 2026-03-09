package org.ilias.influapp.services;

import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.entities.Reaction;

import java.util.List;

public interface EmojiSentimentAnalyzer {

    PostSentiment analyzeEmojiSentiment(List<Reaction> reactions, List<String> comments);

    double calculateReactionScore(List<Reaction> reactions);

    double calculateEmojiScore(List<String> comments);
}
