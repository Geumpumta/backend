package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.RankType;
import com.gpt.geumpumtabackend.rank.domain.SeasonRankingSnapshot;
import com.gpt.geumpumtabackend.user.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeasonRankingSnapshotRepository extends JpaRepository<SeasonRankingSnapshot, Long> {


    boolean existsBySeasonId(Long seasonId);


    List<SeasonRankingSnapshot> findBySeasonIdAndRankType(Long seasonId, RankType rankType);


    List<SeasonRankingSnapshot> findBySeasonIdAndRankTypeAndDepartment(
        Long seasonId, RankType rankType, Department department
    );

    int countBySeasonId(Long seasonId);
}
