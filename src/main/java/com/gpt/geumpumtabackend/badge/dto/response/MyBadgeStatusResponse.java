package com.gpt.geumpumtabackend.badge.dto.response;

import com.gpt.geumpumtabackend.badge.domain.Badge;

import java.time.LocalDateTime;

public record MyBadgeStatusResponse(
        String code,
        String name,
        String description,
        String iconUrl,
        boolean owned,
        LocalDateTime awardedAt
) {
    public static MyBadgeStatusResponse from(Badge badge, LocalDateTime awardedAt) {
        return new MyBadgeStatusResponse(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl(),
                awardedAt != null,
                awardedAt
        );
    }
}
