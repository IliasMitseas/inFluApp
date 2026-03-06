package org.ilias.influapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfluencerDashboardDto {
    private Long influencerId;
    private Integer totalPosts;
    private Integer totalCollaborations;
    private Double avgEngagementRate;
    private Integer totalFollowers;
    private Double influencerScore;
    private List<PostSummaryDto> recentPosts;
    private List<PlatformCountDto> platformCounts;
    private List<PostSummaryDto> topPosts;
}
