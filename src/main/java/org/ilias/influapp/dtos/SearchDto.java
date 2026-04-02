package org.ilias.influapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ilias.influapp.entities.Enums.Category;
import org.ilias.influapp.entities.Enums.InfluencerType;
import org.ilias.influapp.entities.Enums.PostSentiment;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SearchDto {
    private String keyword;
    private Category category;
    private InfluencerType type;
    private Integer minFollowers;
    private Integer maxBudget;
    private Boolean isAvailable;
    private String location;
    // New filters
    private Double minEngagement;
    private Double maxEngagement;
    private Double minSentiment;
    private Double maxSentiment;
    private Double minScore;
    private Double maxScore;
    private PostSentiment selectedSentiment;
    private Integer page;
    private Integer size;
    private String sort;
}
