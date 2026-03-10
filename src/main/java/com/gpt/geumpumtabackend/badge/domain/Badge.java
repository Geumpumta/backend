package com.gpt.geumpumtabackend.badge.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    private String name;

    private String description;

    private String iconUrl;

    @Enumerated(EnumType.STRING)
    private BadgeType badgeType;

    private Long thresholdValue;

    @Column(name = "badge_rank")
    private Long rank;

    @Builder
    private Badge(
            String code,
            String name,
            String description,
            String iconUrl,
            BadgeType badgeType,
            Long thresholdValue,
            Long rank
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.badgeType = badgeType;
        this.thresholdValue = thresholdValue;
        this.rank = rank;
    }
}
