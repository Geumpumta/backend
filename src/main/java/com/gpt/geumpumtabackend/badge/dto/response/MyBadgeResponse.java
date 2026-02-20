package com.gpt.geumpumtabackend.badge.dto.response;

import java.time.LocalDateTime;

public record MyBadgeResponse(
        String code,
        String name,
        String description,
        String iconUrl,
        LocalDateTime awardedAt
) {
}
