package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
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
    @Query("""
            SELECT new com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp(
                        dr.department,
                        dr.totalMillis,
                        dr.rank
                        )
            FROM DepartmentRanking  dr
            WHERE dr.calculatedAt =:period
            ORDER BY dr.rank ASC 
                        """)
    List<DepartmentRankingTemp> getFinishedDepartmentRanking(@Param("period") LocalDateTime period);
}
