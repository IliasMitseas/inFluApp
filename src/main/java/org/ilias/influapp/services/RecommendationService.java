package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ilias.influapp.dto.RecommendedInfluencerDto;
import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.Enums.AgeGroup;
import org.ilias.influapp.entities.Enums.GenderGroup;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.PostRepository;
import org.ilias.influapp.repository.SentimentAnalysisRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final InfluencerRepository influencerRepository;
    private final PostRepository postRepository;
    private final SentimentAnalysisRepository sentimentAnalysisRepository;
    private final BusinessRepository businessRepository;


    /**
     * Legacy recommendation method for backward compatibility.
     * Recommend influencers based on keyword, location, age group, and gender target.
     */
    public List<RecommendedInfluencerDto> recommend(String keyword, String location, AgeGroup ageGroup, GenderGroup genderTarget, int limit) {
        return recommendForBusiness(null, keyword, location, ageGroup, genderTarget, limit);
    }

    /**
     * Enhanced recommendation method with Business profile awareness for brand fit.
     * Takes Business profile preferences into account for better matching.
     *
     * @param businessId ID of the business looking for influencers (nullable for backward compat)
     * @param keyword Search keyword for content matching
     * @param location Geographic location filter
     * @param ageGroup Target age group
     * @param genderTarget Target gender group
     * @param limit Maximum number of recommendations
     * @return List of recommended influencers with enhanced scoring
     */
    public List<RecommendedInfluencerDto> recommendForBusiness(Long businessId, String keyword, String location,
                                                               AgeGroup ageGroup, GenderGroup genderTarget, int limit) {
        // Fetch business profile if provided
        Business business = null;
        if (businessId != null) {
            business = businessRepository.findById(businessId).orElse(null);
        }

        // Fetch candidates
        List<Influencer> candidates = influencerRepository.findAll(PageRequest.of(0, 200)).getContent();
        log.info("Recommendation requested: businessId={}, keyword='{}', location='{}', ageGroup={}, genderTarget={}, limit='{}'",
                 businessId, keyword, location, ageGroup, genderTarget, limit);
        log.info("Loaded {} candidate influencers (pre-filter)", candidates.size());

        // Hard filters: must be available, and budget must be compatible
        List<Influencer> filtered = new ArrayList<>();
        for (Influencer inf : candidates) {
            // Hard filter 1: Must be available
            if (!Boolean.TRUE.equals(inf.getIsAvailable())) {
                log.debug("Filtering out influencer {} - not available", inf.getId());
                continue;
            }

            // Hard filter 2: Budget compatibility (if business has max budget)
            if (business != null && business.getMaxBudgetPerCollaboration() != null) {
                if (inf.getMinCollaborationBudget() != null &&
                    inf.getMinCollaborationBudget() > business.getMaxBudgetPerCollaboration()) {
                    log.debug("Filtering out influencer {} - budget incompatible ({} > {})",
                              inf.getId(), inf.getMinCollaborationBudget(), business.getMaxBudgetPerCollaboration());
                    continue;
                }
            }

            // Soft filter: Location (can pass but will affect scoring)
            if (location != null && !location.isBlank()) {
                if (inf.getLocation() == null || !inf.getLocation().toLowerCase().contains(location.toLowerCase())) {
                    // Location doesn't match - could skip or continue with lower score
                    // For now, we continue but will score lower
                }
            }

            filtered.add(inf);
        }
        log.info("{} candidates remain after hard filters", filtered.size());

        // Compute raw features for scoring
        List<CandidateFeatures> feats = new ArrayList<>();
        int maxKeywordMatches = 1;
        int maxPostCount = 1;

        for (Influencer inf : filtered) {
            long matches = 0;
            if (keyword != null && !keyword.isBlank()) {
                matches = postRepository.countBySocialMediaInfluencerIdAndContentContainingIgnoreCase(inf.getId(), keyword);
            }
            long postCount = postRepository.countBySocialMediaInfluencerId(inf.getId());
            maxKeywordMatches = (int) Math.max(maxKeywordMatches, matches);
            maxPostCount = (int) Math.max(maxPostCount, postCount);
            feats.add(new CandidateFeatures(inf, matches, postCount));
        }

        List<RecommendedInfluencerDto> results = new ArrayList<>();
        StringBuilder reasons = new StringBuilder();

        for (CandidateFeatures f : feats) {
            Influencer inf = f.influencer;
            reasons.setLength(0); // Reset reasons for this influencer

            // ===== LEGACY SCORES (for backward compatibility) =====
            double keywordScore = (double) f.keywordMatches / Math.max(1, maxKeywordMatches);

            Double avgPol = null;
            if (keyword != null && !keyword.isBlank()) {
                avgPol = sentimentAnalysisRepository.avgPolarityByInfluencerIdAndKeyword(inf.getId(), keyword.toLowerCase());
            }
            double aspectSentimentScore = 0.5;
            if (avgPol != null) {
                aspectSentimentScore = (avgPol + 1.0) / 2.0;
            }

            double engagementScore = 0.0;
            if (inf.getEngagementRate() != null) {
                engagementScore = Math.min(inf.getEngagementRate().doubleValue(), 20.0) / 20.0;
            }

            double activityScore = (double) f.postCount / Math.max(1, maxPostCount);
            double influencerScore = inf.getInfluencerScore() != null ? Math.min(inf.getInfluencerScore(), 100.0) / 100.0 : 0.0;

            // ===== NEW BRAND FIT SCORES =====

            // 1. Category Match (25%)
            double categoryMatchScore = 0.5; // neutral baseline
            if (business != null && business.getTargetCategory() != null && inf.getCategory() != null) {
                categoryMatchScore = (inf.getCategory() == business.getTargetCategory()) ? 1.0 : 0.0;
                if (categoryMatchScore == 1.0) reasons.append("✓ Perfect category match | ");
            }

            // 2. Audience Fit (20%) - combine age and gender
            double ageAudienceFit = 0.5;
            double genderAudienceFit = 0.5;

            AgeGroup targetAge = (business != null && business.getTargetAgeGroup() != null) ? business.getTargetAgeGroup() : ageGroup;
            if (targetAge != null && inf.getAgeGroup() != null) {
                if (inf.getAgeGroup() == targetAge) {
                    ageAudienceFit = 1.0;
                } else if (inf.getAgeGroup() == AgeGroup.ALL_AGES) {
                    ageAudienceFit = 0.9;
                } else {
                    ageAudienceFit = 0.1;
                }
            }

            GenderGroup targetGender = (business != null && business.getTargetGenderGroup() != null) ? business.getTargetGenderGroup() : genderTarget;
            if (targetGender != null && inf.getGenderTarget() != null) {
                if (inf.getGenderTarget() == targetGender) {
                    genderAudienceFit = 1.0;
                } else if (inf.getGenderTarget() == GenderGroup.ANY) {
                    genderAudienceFit = 0.9;
                } else {
                    genderAudienceFit = 0.1;
                }
            }

            double audienceFitScore = (ageAudienceFit + genderAudienceFit) / 2.0;

            // 3. Influencer Type Match (12%)
            double influencerTypeMatchScore = 0.5;
            if (business != null && business.getPreferredInfluencerType() != null && inf.getInfluencerType() != null) {
                influencerTypeMatchScore = (inf.getInfluencerType() == business.getPreferredInfluencerType()) ? 1.0 : 0.4;
            }

            // 4. Availability Score (10%) - should always be high since we hard-filtered
            double availabilityScore = Boolean.TRUE.equals(inf.getIsAvailable()) ? 1.0 : 0.0;

            // 5. Budget Compatibility (10%)
            boolean budgetCompatible = true;
            double budgetCompatibilityScore = 0.5;
            if (business != null && business.getMaxBudgetPerCollaboration() != null) {
                if (inf.getMinCollaborationBudget() != null &&
                    inf.getMinCollaborationBudget() <= business.getMaxBudgetPerCollaboration()) {
                    budgetCompatibilityScore = 1.0;
                    reasons.append("✓ Budget compatible | ");
                } else {
                    budgetCompatible = false;
                }
            }

            // 6. Location Match (5%)
            double locationMatchScore = 0.5;
            if (location != null && !location.isBlank()) {
                if (inf.getLocation() != null && inf.getLocation().toLowerCase().contains(location.toLowerCase())) {
                    locationMatchScore = 1.0;
                    reasons.append("✓ Location match | ");
                } else {
                    locationMatchScore = 0.1;
                }
            }

            // ===== FINAL SCORE CALCULATION =====
            // NEW weights: brand-fit focused
            double finalScore = 0.25 * categoryMatchScore
                    + 0.20 * audienceFitScore
                    + 0.18 * engagementScore
                    + 0.12 * influencerTypeMatchScore
                    + 0.10 * availabilityScore
                    + 0.10 * budgetCompatibilityScore
                    + 0.05 * locationMatchScore;

            // Add default reason if none collected
            if (reasons.length() == 0) {
                reasons.append("Good engagement & overall fit");
            }

            // Build DTO with all scores
            RecommendedInfluencerDto dto = RecommendedInfluencerDto.builder()
                    .influencer(inf)
                    .score(Math.round(finalScore * 10000.0) / 10000.0) // Round to 4 decimals
                    .keywordScore(keywordScore)
                    .aspectSentimentScore(aspectSentimentScore)
                    .engagementScore(engagementScore)
                    .activityScore(activityScore)
                    .influencerScore(influencerScore)
                    .ageMatchScore(ageAudienceFit)
                    .genderMatchScore(genderAudienceFit)
                    .categoryMatchScore(Math.round(categoryMatchScore * 10000.0) / 10000.0)
                    .audienceFitScore(Math.round(audienceFitScore * 10000.0) / 10000.0)
                    .influencerTypeMatchScore(Math.round(influencerTypeMatchScore * 10000.0) / 10000.0)
                    .availabilityScore(availabilityScore)
                    .budgetCompatibilityScore(Math.round(budgetCompatibilityScore * 10000.0) / 10000.0)
                    .locationMatchScore(Math.round(locationMatchScore * 10000.0) / 10000.0)
                    .budgetCompatible(budgetCompatible)
                    .recommendationReason(reasons.toString())
                    .build();

            results.add(dto);
        }

        results.sort(Comparator.comparingDouble(RecommendedInfluencerDto::getScore).reversed());

        if (results.size() > limit) {
            var out = results.subList(0, limit);
            log.info("Returning {} recommendations (trimmed to limit={})", out.size(), limit);
            log.info("Top recommendation ids={}", out.stream().map(r -> r.getInfluencer().getId()).toList());
            return out;
        }

        log.info("Returning {} recommendations", results.size());
        log.info("Recommendation ids={}", results.stream().map(r -> r.getInfluencer().getId()).toList());
        return results;
    }

    private static class CandidateFeatures {
        Influencer influencer;
        int keywordMatches;
        int postCount;

        CandidateFeatures(Influencer influencer, long keywordMatches, long postCount) {
            this.influencer = influencer;
            this.keywordMatches = (int) keywordMatches;
            this.postCount = (int) postCount;
        }
    }
}

