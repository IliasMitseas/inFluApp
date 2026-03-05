package org.ilias.influapp.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ilias.influapp.entities.Reaction;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    @NotBlank(message = "Content is required")
    private String content;
    private List<Reaction> reactions;
    private List<String> comments;
    @Min(value = 0, message = "Shares cannot be negative")
    private Integer shares;
    @Min(value = 0, message = "Reach cannot be negative")
    private Integer reach;
    @Min(value = 0, message = "Impressions cannot be negative")
    private Integer impressionCount;
}
