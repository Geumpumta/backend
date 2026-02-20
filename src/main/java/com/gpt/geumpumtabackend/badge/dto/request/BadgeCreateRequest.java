package com.gpt.geumpumtabackend.badge.dto.request;

import com.gpt.geumpumtabackend.badge.domain.BadgeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BadgeCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String iconUrl,
        @NotNull BadgeType badgeType,
        Long thresholdValue,
        Long rank
) {
}
