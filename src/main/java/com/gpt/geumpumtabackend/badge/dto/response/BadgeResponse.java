package com.gpt.geumpumtabackend.badge.dto.response;

import com.gpt.geumpumtabackend.badge.domain.Badge;
import com.gpt.geumpumtabackend.badge.domain.BadgeType;

public record BadgeResponse(
        Long id,
        String code,
        String name,
        String description,
        String iconUrl,
        BadgeType badgeType,
        Long thresholdValue,
        Long rank
) {
    public static BadgeResponse from(Badge badge) {
        return new BadgeResponse(
                badge.getId(),
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl(),
                badge.getBadgeType(),
                badge.getThresholdValue(),
                badge.getRank()
        );
    }
}
