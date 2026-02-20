package com.gpt.geumpumtabackend.badge.dto.response;

import com.gpt.geumpumtabackend.badge.domain.Badge;

public record NewBadgeResponse(
        String code,
        String name,
        String description,
        String iconUrl
) {
    public static NewBadgeResponse from(Badge badge) {
        return new NewBadgeResponse(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl()
        );
    }
}
