package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DepartmentRankingRepository extends JpaRepository<DepartmentRanking, Long> {


    /*
    끝난 학과
     */
    @Query(value = """
            SELECT dr.department as department,
                   dr.total_millis as totalMillis,
                   dr.ranking as ranking
            FROM department_ranking dr
            WHERE dr.calculated_at = :period
            AND dr.ranking_type = :rankingType
            ORDER BY dr.ranking ASC 
            """, nativeQuery = true)
    List<DepartmentRankingTemp> getFinishedDepartmentRanking(@Param("period") LocalDateTime period, @Param("rankingType") RankingType rankingType);
}
