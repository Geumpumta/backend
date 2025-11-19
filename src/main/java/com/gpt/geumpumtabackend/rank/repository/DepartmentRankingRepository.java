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
            WITH all_departments AS (
                SELECT 'ARCHITECTURE_ENGINEERING' as dept
                UNION ALL SELECT 'ARCHITECTURE'
                UNION ALL SELECT 'CIVIL_ENGINEERING'
                UNION ALL SELECT 'ENVIRONMENTAL_ENGINEERING'
                UNION ALL SELECT 'MECHANICAL_ENGINEERING'
                UNION ALL SELECT 'MECHANICAL_SYSTEMS_ENGINEERING'
                UNION ALL SELECT 'SMART_MOBILITY'
                UNION ALL SELECT 'INDUSTRIAL_ENGINEERING'
                UNION ALL SELECT 'APPLIED_MATH_BIGDATA'
                UNION ALL SELECT 'POLYMER_ENGINEERING'
                UNION ALL SELECT 'MATERIALS_ENGINEERING'
                UNION ALL SELECT 'SEMICONDUCTOR_SYSTEMS'
                UNION ALL SELECT 'ELECTRONIC_SYSTEMS'
                UNION ALL SELECT 'SOFTWARE'
                UNION ALL SELECT 'ARTIFICIAL_INTELLIGENCE'
                UNION ALL SELECT 'COMPUTER_ENGINEERING'
                UNION ALL SELECT 'MATERIALS_DESIGN_ENGINEERING'
                UNION ALL SELECT 'CHEMICAL_ENGINEERING'
                UNION ALL SELECT 'CHEMICAL_BIO_MATERIALS'
                UNION ALL SELECT 'OPTICAL_SYSTEMS'
                UNION ALL SELECT 'BIOMEDICAL_ENGINEERING'
                UNION ALL SELECT 'IT_CONVERGENCE'
                UNION ALL SELECT 'LIBERAL_MAJOR'
                UNION ALL SELECT 'BUSINESS_ADMINISTRATION'
            )
            SELECT d.dept as department,
                   COALESCE(dr.total_millis, 0) as totalMillis,
                   RANK() OVER (ORDER BY COALESCE(dr.total_millis, 0) DESC) as ranking
            FROM all_departments d
            LEFT JOIN department_ranking dr ON d.dept = dr.department
                AND dr.calculated_at = :period
                AND dr.ranking_type = :rankingType
            ORDER BY COALESCE(dr.total_millis, 0) DESC
            """, nativeQuery = true)
    List<DepartmentRankingTemp> getFinishedDepartmentRanking(@Param("period") LocalDateTime period, @Param("rankingType") String rankingType);
}
