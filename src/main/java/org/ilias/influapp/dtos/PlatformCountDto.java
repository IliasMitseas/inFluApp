package org.ilias.influapp.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ilias.influapp.entities.Enums.Platform;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformCountDto {
    private Platform platform;
    private Long count;
}
