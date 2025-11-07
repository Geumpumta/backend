package com.gpt.geumpumtabackend.rank.domain;

import com.gpt.geumpumtabackend.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class UserRanking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, name = "ranking")
    private Long rank;

    @Column(nullable = false)
    private Long totalMillis;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RankingType rankingType;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @Builder
    public UserRanking(Long id, User user, Long rank, Long totalMillis, RankingType rankingType, LocalDateTime calculatedAt) {
        this.id = id;
        this.user = user;
        this.rank = rank;
        this.totalMillis = totalMillis;
        this.rankingType = rankingType;
        this.calculatedAt = calculatedAt;
    }
}
