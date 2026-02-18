package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.RankType;
import com.gpt.geumpumtabackend.rank.domain.SeasonRankingSnapshot;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.user.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonRankingSnapshotRepository extends JpaRepository<SeasonRankingSnapshot, Long> {


    boolean existsBySeasonId(Long seasonId);


    List<SeasonRankingSnapshot> findBySeasonIdAndRankType(Long seasonId, RankType rankType);


    List<SeasonRankingSnapshot> findBySeasonIdAndRankTypeAndDepartment(
        Long seasonId, RankType rankType, Department department
    );

    int countBySeasonId(Long seasonId);


    @Query(value = """
            SELECT s.department as department,
                   CAST(SUM(s.final_total_millis) AS SIGNED) as totalMillis,
                   0 as ranking
            FROM season_ranking_snapshot s
            WHERE s.season_id = :seasonId
              AND s.rank_type = 'DEPARTMENT'
              AND s.final_rank <= 30
            GROUP BY s.department
            ORDER BY SUM(s.final_total_millis) DESC
            """, nativeQuery = true)
    List<DepartmentRankingTemp> aggregateDepartmentRankingBySeasonId(@Param("seasonId") Long seasonId);
}
