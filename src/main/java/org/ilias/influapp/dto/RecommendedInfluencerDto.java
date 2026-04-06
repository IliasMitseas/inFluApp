package org.ilias.influapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.ilias.influapp.entities.Influencer;

@Data
@AllArgsConstructor
@Builder
public class RecommendedInfluencerDto {
    private Influencer influencer;
    private double score;

    // Legacy scores (kept for backward compatibility)
    private double keywordScore;
    private double aspectSentimentScore;
    private double engagementScore;
    private double activityScore;
    private double influencerScore;
    private double ageMatchScore;
    private double genderMatchScore;

    // Enhanced Brand Fit Scores
    private double categoryMatchScore;
    private double audienceFitScore;
    private double influencerTypeMatchScore;
    private double availabilityScore;
    private double budgetCompatibilityScore;
    private double locationMatchScore;

    // Meta information
    private boolean budgetCompatible;
    private String recommendationReason;

}

