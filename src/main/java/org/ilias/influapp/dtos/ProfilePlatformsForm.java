package org.ilias.influapp.dtos;

import lombok.Data;
import org.ilias.influapp.entities.Enums.Platform;

import java.util.EnumSet;

@Data
public class ProfilePlatformsForm {
    private EnumSet<Platform> selectedPlatforms = EnumSet.noneOf(Platform.class);
}
