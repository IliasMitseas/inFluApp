package org.ilias.influapp.services;

import org.ilias.influapp.entities.PostSentiment;
import org.ilias.influapp.entities.Reaction;

import java.util.List;

public interface SentimentAnalyzer {


    PostSentiment analyzeSentiment(List<Reaction> reactions, List<String> comments);

    double calculateReactionScore(List<Reaction> reactions);

    double calculateCommentScore(List<String> comments);
}
