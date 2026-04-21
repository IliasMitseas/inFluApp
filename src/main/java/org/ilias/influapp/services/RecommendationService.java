package org.ilias.influapp.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ilias.influapp.dto.RecommendedInfluencerDto;
import org.ilias.influapp.entities.Business;
import org.ilias.influapp.entities.Campaign;
import org.ilias.influapp.entities.Enums.AgeGroup;
import org.ilias.influapp.entities.Enums.GenderGroup;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.CampaignRepository;
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

    // Versioned scoring contract for reproducibility in evaluations.
    private static final String SCORE_POLICY_VERSION = "v2.2-balanced-fit";

    // Centralized weights (must sum to 1.0)
    private static final double W_CATEGORY_MATCH = 0.30;
    private static final double W_AUDIENCE_FIT = 0.18;
    private static final double W_ENGAGEMENT = 0.12;
    private static final double W_CREATOR_QUALITY = 0.10;
    private static final double W_INFLUENCER_TYPE = 0.10;
    private static final double W_AVAILABILITY = 0.08;
    private static final double W_BUDGET_COMPATIBILITY = 0.08;
    private static final double W_LOCATION_MATCH = 0.04;

    private final InfluencerRepository influencerRepository;
    private final PostRepository postRepository;
    private final SentimentAnalysisRepository sentimentAnalysisRepository;
    private final BusinessRepository businessRepository;
    private final CampaignRepository campaignRepository;

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 5;



    public List<RecommendedInfluencerDto> recommend(String keyword, String location, AgeGroup ageGroup, GenderGroup genderTarget, int limit) {
        return recommendForBusiness(null, null, keyword, location, ageGroup, genderTarget, limit);
    }

    public List<RecommendedInfluencerDto> recommendForBusiness(Long businessId, String keyword, String location,
                                                               AgeGroup ageGroup, GenderGroup genderTarget, int limit) {
        return recommendForBusiness(businessId, null, keyword, location, ageGroup, genderTarget, limit);
    }


    public List<RecommendedInfluencerDto> recommendForBusiness(Long businessId, Long campaignId, String keyword, String location,
                                                               AgeGroup ageGroup, GenderGroup genderTarget, int limit) {
        int effectiveLimit = normalizeLimit(limit);

        // Fetch business profile if provided
        Business business = null;
        if (businessId != null) {
            business = businessRepository.findById(businessId).orElse(null);
        }

        Campaign campaign = null;
        if (campaignId != null && businessId != null) {
            campaign = campaignRepository.findById(campaignId)
                    .filter(c -> c.getBusiness() != null && businessId.equals(c.getBusiness().getId()))
                    .orElse(null);
            if (campaign == null) {
                log.warn("Campaign {} not found for business {} - falling back to business-only scoring", campaignId, businessId);
            }
        }

        Integer budgetCap = resolveBudgetCap(business, campaign);

        if (business != null) {
            log.info("Business context loaded: id={}, targetCategory={}, targetAgeGroup={}, targetGenderGroup={}, preferredInfluencerType={}, maxBudgetPerCollaboration={}",
                    business.getId(),
                    business.getTargetCategory(),
                    business.getTargetAgeGroup(),
                    business.getTargetGenderGroup(),
                    business.getPreferredInfluencerType(),
                    business.getMaxBudgetPerCollaboration());
        }

        if (campaign != null) {
            log.info("Campaign context loaded: id={}, title='{}', targetCategory={}, budget={} (effectiveBudgetCap={})",
                    campaign.getId(), campaign.getTitle(), campaign.getTargetCategory(), campaign.getBudget(), budgetCap);
        } else {
            log.info("No campaign context applied (effectiveBudgetCap={})", budgetCap);
        }

        // Fetch candidates
        List<Influencer> candidates = influencerRepository.findAll(PageRequest.of(0, 200)).getContent();
        log.info("Recommendation requested: businessId={}, campaignId={}, keyword='{}', location='{}', ageGroup={}, genderTarget={}, limit='{}'",
                 businessId, campaignId, keyword, location, ageGroup, genderTarget, effectiveLimit);
        log.info("Loaded {} candidate influencers (pre-filter)", candidates.size());

        // Keep hard filters minimal to avoid empty recommendations.
        List<Influencer> available = new ArrayList<>();
        for (Influencer inf : candidates) {
            if (Boolean.TRUE.equals(inf.getIsAvailable())) {
                available.add(inf);
            }
        }

        List<Influencer> budgetCompatible = new ArrayList<>();
        if (budgetCap != null) {
            for (Influencer inf : available) {
                if (inf.getMinCollaborationBudget() == null || inf.getMinCollaborationBudget() <= budgetCap) {
                    budgetCompatible.add(inf);
                }
            }
        }

        List<Influencer> budgetStage = (budgetCap != null && !budgetCompatible.isEmpty()) ? budgetCompatible : available;
        if (budgetCap != null && budgetCompatible.isEmpty()) {
            log.warn("No candidates satisfy strict budget cap={} - falling back to available influencers with budget penalty scoring", budgetCap);
        }

        List<Influencer> locationCompatible = new ArrayList<>();
        if (location != null && !location.isBlank()) {
            for (Influencer inf : budgetStage) {
                boolean locationMatches = inf.getLocation() != null
                        && inf.getLocation().toLowerCase().contains(location.toLowerCase());
                if (locationMatches) {
                    locationCompatible.add(inf);
                }
            }
        }

        List<Influencer> filtered = (location != null && !location.isBlank() && !locationCompatible.isEmpty())
                ? locationCompatible
                : budgetStage;
        if (location != null && !location.isBlank() && locationCompatible.isEmpty()) {
            log.warn("No candidates match strict location='{}' - falling back to broader geography with location penalty scoring", location);
        }

        log.info("{} candidates remain after adaptive hard filters", filtered.size());

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
            if (campaign != null && campaign.getTargetCategory() != null && inf.getCategory() != null) {
                categoryMatchScore = (inf.getCategory() == campaign.getTargetCategory()) ? 1.0 : 0.0;
                if (categoryMatchScore == 1.0) reasons.append("✓ Campaign category match | ");
            }

            // 2. Audience Fit (20%) - combine age and gender
            double ageAudienceFit = 0.5;
            double genderAudienceFit = 0.5;

            AgeGroup targetAge = (business != null && business.getTargetAgeGroup() != null) ? business.getTargetAgeGroup() : ageGroup;
            if (targetAge != null && inf.getAgeGroup() != null) {
                if (inf.getAgeGroup() == targetAge) {
                    ageAudienceFit = 1.0;
                } else if (inf.getAgeGroup() == AgeGroup.ALL_AGES) {
                    ageAudienceFit = 0.75;
                } else {
                    ageAudienceFit = 0.1;
                }
            }

            GenderGroup targetGender = (business != null && business.getTargetGenderGroup() != null) ? business.getTargetGenderGroup() : genderTarget;
            if (targetGender != null && inf.getGenderTarget() != null) {
                if (inf.getGenderTarget() == targetGender) {
                    genderAudienceFit = 1.0;
                } else if (inf.getGenderTarget() == GenderGroup.ANY) {
                    genderAudienceFit = 0.7;
                } else {
                    genderAudienceFit = 0.1;
                }
            }

            double audienceFitScore = (ageAudienceFit + genderAudienceFit) / 2.0;
            if (targetAge != null) {
                reasons.append(ageAudienceFit >= 0.9 ? "✓ Age fit | " : "✗ Age mismatch | ");
            }
            if (targetGender != null) {
                reasons.append(genderAudienceFit >= 0.9 ? "✓ Gender audience fit | " : "✗ Gender audience mismatch | ");
            }

            // 3. Influencer Type Match (12%)
            double influencerTypeMatchScore = 0.5;
            if (business != null && business.getPreferredInfluencerType() != null && inf.getInfluencerType() != null) {
                influencerTypeMatchScore = (inf.getInfluencerType() == business.getPreferredInfluencerType()) ? 1.0 : 0.2;
                reasons.append(influencerTypeMatchScore == 1.0 ? "✓ Influencer type fit | " : "✗ Influencer type mismatch | ");
            }

            // 4. Availability Score (10%) - should always be high since we hard-filtered
            double availabilityScore = Boolean.TRUE.equals(inf.getIsAvailable()) ? 1.0 : 0.0;

            // 5. Budget Compatibility (10%)
            boolean isBudgetCompatible = true;
            double budgetCompatibilityScore = 0.5;
            if (budgetCap != null) {
                if (inf.getMinCollaborationBudget() != null && inf.getMinCollaborationBudget() <= budgetCap) {
                    budgetCompatibilityScore = 1.0;
                    reasons.append("✓ Budget compatible | ");
                } else {
                    isBudgetCompatible = false;
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
            double finalScore = calculateFinalScore(
                    categoryMatchScore,
                    audienceFitScore,
                    engagementScore,
                    influencerScore,
                    influencerTypeMatchScore,
                    availabilityScore,
                    budgetCompatibilityScore,
                    locationMatchScore
            );

            // Cap score if business has explicit category target and influencer mismatches it.
            if (business != null && business.getTargetCategory() != null && inf.getCategory() != null
                    && inf.getCategory() != business.getTargetCategory()) {
                finalScore = Math.min(finalScore, 0.55);
                reasons.append("✗ Category mismatch cap applied | ");
            }

            // Add default reason if none collected
            if (reasons.isEmpty()) {
                reasons.append("Good engagement & overall fit");
            }
            reasons.append(" | policy=").append(SCORE_POLICY_VERSION);
            reasons.append(String.format(" | components[c=%.2f,a=%.2f,e=%.2f,q=%.2f,t=%.2f,b=%.2f,l=%.2f]",
                    categoryMatchScore,
                    audienceFitScore,
                    engagementScore,
                    influencerScore,
                    influencerTypeMatchScore,
                    budgetCompatibilityScore,
                    locationMatchScore));
            if (campaign != null) {
                reasons.append(" | campaign=").append(campaign.getTitle());
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
                    .budgetCompatible(isBudgetCompatible)
                    .recommendationReason(reasons.toString())
                    .build();

            results.add(dto);
        }

        results.sort(Comparator.comparingDouble(RecommendedInfluencerDto::getScore).reversed());

        if (results.size() > effectiveLimit) {
            var out = results.subList(0, effectiveLimit);
            log.info("Returning {} recommendations (trimmed to limit={})", out.size(), effectiveLimit);
            log.info("Top recommendation ids={}", out.stream().map(r -> r.getInfluencer().getId()).toList());
            logTopRecommendations(out, businessId, campaignId);
            return out;
        }

        log.info("Returning {} recommendations", results.size());
        log.info("Recommendation ids={}", results.stream().map(r -> r.getInfluencer().getId()).toList());
        logTopRecommendations(results, businessId, campaignId);
        return results;
    }

    private void logTopRecommendations(List<RecommendedInfluencerDto> recs, Long businessId, Long campaignId) {
        int previewSize = Math.min(5, recs.size());
        if (previewSize == 0) {
            return;
        }

        log.info("Top {} recommendations preview (businessId={}, campaignId={}):", previewSize, businessId, campaignId);
        for (int i = 0; i < previewSize; i++) {
            var r = recs.get(i);
            log.info("  #{} id={} user={} score={} [category={}, audience={}, engagement={}, type={}, budget={}, location={}] reason={}",
                    i + 1,
                    r.getInfluencer().getId(),
                    r.getInfluencer().getUsername(),
                    r.getScore(),
                    r.getCategoryMatchScore(),
                    r.getAudienceFitScore(),
                    r.getEngagementScore(),
                    r.getInfluencerTypeMatchScore(),
                    r.getBudgetCompatibilityScore(),
                    r.getLocationMatchScore(),
                    r.getRecommendationReason());
        }
    }

    private double calculateFinalScore(double categoryMatchScore,
                                       double audienceFitScore,
                                       double engagementScore,
                                       double influencerScore,
                                       double influencerTypeMatchScore,
                                       double availabilityScore,
                                       double budgetCompatibilityScore,
                                       double locationMatchScore) {
        double weighted = W_CATEGORY_MATCH * categoryMatchScore
                + W_AUDIENCE_FIT * audienceFitScore
                + W_ENGAGEMENT * engagementScore
                + W_CREATOR_QUALITY * influencerScore
                + W_INFLUENCER_TYPE * influencerTypeMatchScore
                + W_AVAILABILITY * availabilityScore
                + W_BUDGET_COMPATIBILITY * budgetCompatibilityScore
                + W_LOCATION_MATCH * locationMatchScore;

        return Math.max(0.0, Math.min(1.0, weighted));
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

    private Integer resolveBudgetCap(Business business, Campaign campaign) {
        Integer businessCap = business != null ? business.getMaxBudgetPerCollaboration() : null;
        Integer campaignCap = (campaign != null && campaign.getBudget() != null)
                ? (int) Math.floor(campaign.getBudget())
                : null;

        if (businessCap == null) return campaignCap;
        if (campaignCap == null) return businessCap;
        return Math.min(businessCap, campaignCap);
    }

    private int normalizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }
}
