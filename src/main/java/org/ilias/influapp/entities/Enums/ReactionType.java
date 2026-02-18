package org.ilias.influapp.entities.Enums;

import lombok.Getter;

@Getter
public enum ReactionType {
    LIKE("👍"),
    LOVE("❤️"),
    HAHA("😂"),
    WOW("😮"),
    SAD("😢"),
    ANGRY("😡");

    private final String emoji;

    ReactionType(String emoji) {
        this.emoji = emoji;
    }
}