package com.gpt.geumpumtabackend.rank.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import com.gpt.geumpumtabackend.user.domain.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeasonRankingSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, name = "season_id")
    private Long seasonId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, name = "rank_type")
    private RankType rankType;

    @Column(nullable = false, name = "final_rank")
    private Integer finalRank;

    @Column(nullable = false, name = "final_total_millis")
    private Long finalTotalMillis;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Department department;

    @Column(nullable = false, name = "snapshot_at")
    private LocalDateTime snapshotAt;

    @Builder
    public SeasonRankingSnapshot(Long seasonId, Long userId, RankType rankType,
                                 Integer finalRank, Long finalTotalMillis,
                                 Department department, LocalDateTime snapshotAt) {
        this.seasonId = seasonId;
        this.userId = userId;
        this.rankType = rankType;
        this.finalRank = finalRank;
        this.finalTotalMillis = finalTotalMillis;
        this.department = department;
        this.snapshotAt = snapshotAt;
    }
}
