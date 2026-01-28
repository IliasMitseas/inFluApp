package org.ilias.influapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ilias.influapp.entities.PostSentiment;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private String content;
    private Integer likes;
    private List<String> comments;
    private Integer shares;
    private Integer reach;
    private Integer impressionCount;
    private PostSentiment postSentiment;
}
