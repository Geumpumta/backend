package com.gpt.geumpumtabackend.rank.domain;

import com.gpt.geumpumtabackend.user.domain.Department;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class DepartmentRanking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private Long totalMillis;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RankingType rankingType;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @Builder
    public DepartmentRanking(Department department, Integer rank, Long totalMillis, RankingType rankingType, LocalDateTime calculatedAt) {
        this.department = department;
        this.rank = rank;
        this.totalMillis = totalMillis;
        this.rankingType = rankingType;
        this.calculatedAt = calculatedAt;
    }
}
