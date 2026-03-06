package org.ilias.influapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignMetricDto {
    private Long campaignId;
    private String title;
    private Double budget;
    private Double remainingBudget;
    private Integer collaborationsCount;
}

