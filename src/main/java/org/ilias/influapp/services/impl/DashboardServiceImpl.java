package org.ilias.influapp.services.impl;

import lombok.RequiredArgsConstructor;
import org.ilias.influapp.dtos.*;
import org.ilias.influapp.entities.Campaign;
import org.ilias.influapp.entities.Post;
import org.ilias.influapp.entities.Influencer;
import org.ilias.influapp.entities.Business;
import org.ilias.influapp.repository.CampaignRepository;
import org.ilias.influapp.repository.CollaborationRepository;
import org.ilias.influapp.repository.InfluencerRepository;
import org.ilias.influapp.repository.BusinessRepository;
import org.ilias.influapp.repository.PostRepository;
import org.ilias.influapp.services.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final InfluencerRepository influencerRepository;
    private final CollaborationRepository collaborationRepository;
    private final CampaignRepository campaignRepository;
    private final BusinessRepository businessRepository;
    private final PostRepository postRepository;


    @Override
    @Transactional(readOnly = true)
    public InfluencerDashboardDto getInfluencerDashboard(Long influencerId, LocalDate from, LocalDate to) {

        Influencer influencer = influencerRepository.findById(influencerId).orElse(null);

        if (influencer == null) {
            return InfluencerDashboardDto.builder().influencerId(influencerId).build();
        }

        // Use repository aggregates where possible
        long totalPosts = postRepository.countBySocialMediaInfluencerId(influencerId);
        Double avgEng = postRepository.avgEngagementRateByInfluencerId(influencerId);
        int totalCollaborations = (int) collaborationRepository.countByInfluencerId(influencerId);

        // recent posts
        List<Post> recentPosts = postRepository.findTop5BySocialMediaInfluencerIdOrderByIdDesc(influencerId);
        List<PostSummaryDto> recent = recentPosts == null ? new ArrayList<>() : recentPosts.stream().map(p ->
                PostSummaryDto.builder()
                        .postId(p.getId())
                        .contentPreview(p.getContent() == null ? "" : (p.getContent().length() > 100 ? p.getContent().substring(0, 100) + "..." : p.getContent()))
                        .engagementRate(p.getEngagementRate())
                        .platform(p.getSocialMedia() == null || p.getSocialMedia().getPlatform() == null ? null : p.getSocialMedia().getPlatform().name())
                        .build()
        ).collect(Collectors.toList());

        // platform breakdown
        List<PlatformCountDto> platformCounts = postRepository.countByPlatformForInfluencer(influencerId);

        // top posts by engagement
        List<Post> top = postRepository.findTop5BySocialMediaInfluencerIdOrderByEngagementRateDesc(influencerId);
        List<PostSummaryDto> topPosts = top == null ? new ArrayList<>() : top.stream().map(p ->
                PostSummaryDto.builder()
                        .postId(p.getId())
                        .contentPreview(p.getContent() == null ? "" : (p.getContent().length() > 100 ? p.getContent().substring(0, 100) + "..." : p.getContent()))
                        .engagementRate(p.getEngagementRate())
                        .platform(p.getSocialMedia() == null || p.getSocialMedia().getPlatform() == null ? null : p.getSocialMedia().getPlatform().name())
                        .build()
        ).collect(Collectors.toList());

        Integer totalFollowers = influencer.getTotalFollowers() == null ? influencer.updateTotalFollowers() : influencer.getTotalFollowers();

        return InfluencerDashboardDto.builder()
                .influencerId(influencerId)
                .totalPosts((int) totalPosts)
                .totalCollaborations(totalCollaborations)
                .avgEngagementRate(avgEng == null ? 0.0 : avgEng)
                .totalFollowers(totalFollowers)
                .influencerScore(influencer.getInfluencerScore())
                .recentPosts(recent)
                .platformCounts(platformCounts)
                .topPosts(topPosts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessDashboardDto getBusinessDashboard(Long businessId, LocalDate from, LocalDate to) {

        Business business = businessRepository.findById(businessId).orElse(null);

        if (business == null) {
            return BusinessDashboardDto.builder().businessId(businessId).build();
        }

        List<Campaign> campaigns = campaignRepository.findByBusinessId(businessId);

        int totalCampaigns = campaigns == null ? 0 : campaigns.size();

        int activeCampaigns = 0;

        double totalBudget = 0.0;

        double remainingBudget = 0.0;

        List<CampaignMetricDto> metrics = new ArrayList<>();

        if (campaigns != null) {
            for (Campaign c : campaigns) {
                if (c == null) {
                    continue;
                }

                totalBudget += c.getBudget() == null ? 0.0 : c.getBudget();

                remainingBudget += c.getRemainingBudget();

                if (c.isActive()) activeCampaigns++;

                int collabsCount = c.getCollaborations() == null ? 0 : c.getCollaborations().size();
                metrics.add(new CampaignMetricDto(c.getId(), c.getTitle(), c.getBudget(), c.getRemainingBudget(), collabsCount));
            }
        }

        long totalCollaborations = collaborationRepository.countByCampaignBusinessId(businessId);
        Double spent = collaborationRepository.sumPaymentByBusinessId(businessId);

        // pick top campaigns by collaborations
        List<CampaignMetricDto> top = metrics.stream()
                .sorted(Comparator.comparing(CampaignMetricDto::getCollaborationsCount, Comparator.reverseOrder()))
                .limit(5)
                .collect(Collectors.toList());

        return BusinessDashboardDto.builder()
                .businessId(businessId)
                .totalCampaigns(totalCampaigns)
                .activeCampaigns(activeCampaigns)
                .totalCollaborations((int) totalCollaborations)
                .totalBudget(totalBudget)
                .remainingBudget(remainingBudget)
                .topCampaigns(top)
                .build();
    }
}

