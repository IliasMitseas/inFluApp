package org.ilias.influapp.entities;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CountsRequest {
    @Min(value = 0, message = "Like count cannot be negative")
    private Integer likeCount = 0;
    @Min(value = 0, message = "Love count cannot be negative")
    private Integer loveCount = 0;
    @Min(value = 0, message = "Haha count cannot be negative")
    private Integer hahaCount = 0;
    @Min(value = 0, message = "Wow count cannot be negative")
    private Integer wowCount = 0;
    @Min(value = 0, message = "Sad count cannot be negative")
    private Integer sadCount = 0;
    @Min(value = 0, message = "Angry count cannot be negative")
    private Integer angryCount = 0;
}
