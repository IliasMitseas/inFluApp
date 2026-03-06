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
public class BusinessDashboardDto {
    private Long businessId;
    private Integer totalCampaigns;
    private Integer activeCampaigns;
    private Integer totalCollaborations;
    private Double totalBudget;
    private Double remainingBudget;
    private List<CampaignMetricDto> topCampaigns;
}

