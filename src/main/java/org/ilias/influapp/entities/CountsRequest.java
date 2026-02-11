package org.ilias.influapp.entities;

import lombok.Data;

@Data
public class CountsRequest {
    private Integer likeCount = 0;
    private Integer loveCount = 0;
    private Integer hahaCount = 0;
    private Integer wowCount = 0;
    private Integer sadCount = 0;
    private Integer angryCount = 0;
}
