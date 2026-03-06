package org.ilias.influapp.dtos;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostSummaryDto {

    private String platform;
    private Double engagementRate;
    private String contentPreview;
    private Long postId;
}


