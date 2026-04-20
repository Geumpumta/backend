package com.gpt.geumpumtabackend.badge.dto.response;

import com.gpt.geumpumtabackend.badge.domain.Badge;

public record RepresentativeBadgeResponse(
        String code,
        String name,
        String description,
        String iconUrl
) {
    public static RepresentativeBadgeResponse from(Badge badge){
        return new RepresentativeBadgeResponse(
                badge.getCode(),
                badge.getName(),
                badge.getDescription(),
                badge.getIconUrl()
        );
    }
}
