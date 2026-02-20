package com.gpt.geumpumtabackend.badge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "user_badge",
        uniqueConstraints = @UniqueConstraint(name="uk_user_badge", columnNames = {"user_id", "badge_id"})
)
public class UserBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "badge_id")
    Long badgeId;

    private LocalDateTime awardedAt;

    private LocalDateTime notifiedAt;

    public UserBadge(Long userId, Long badgeId, LocalDateTime awardedAt, LocalDateTime notifiedAt) {
        this.userId = userId;
        this.badgeId = badgeId;
        this.awardedAt = awardedAt;
        this.notifiedAt = notifiedAt;
    }

    public void markNotified(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
}
