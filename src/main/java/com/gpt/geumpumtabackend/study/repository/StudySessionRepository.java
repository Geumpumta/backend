package com.gpt.geumpumtabackend.study.repository;

import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.statistics.dto.*;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import com.gpt.geumpumtabackend.study.domain.StudyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {



    Optional<StudySession> findByIdAndUser_Id(Long id, Long userId);

    Optional<StudySession> findByUser_IdAndStatus(Long userId, StudyStatus status);

    // 날짜가 오늘이고, userId와 일치하고, endTime이 null이 아닌 것
    @Query(value = "SELECT COALESCE(SUM(s.total_millis), 0) " +
            "FROM study_session s " +
            "WHERE s.user_id = :userId " +
            "AND s.end_time BETWEEN :startOfDay AND :endOfDay", nativeQuery = true)
    Long sumCompletedStudySessionByUserId(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    @Query(value = "SELECT COALESCE(SUM(s.total_millis), 0) " +
            "FROM study_session s " +
            "WHERE s.user_id = :userId " +
            "AND s.end_time IS NOT NULL", nativeQuery = true)
    Long sumTotalStudyMillisByUserId(@Param("userId") Long userId);



    @EntityGraph(attributePaths = {"user"})
    List<StudySession> findAllByStatusAndStartTimeBefore(StudyStatus status, LocalDateTime now);

    /*
    현재 진행중인 기간의 공부 시간 연산
     */
    @Query(value = """
        SELECT u.id as userId, 
               u.nickname as nickname,
               u.picture as imageUrl,
               u.department as department,
               CAST(COALESCE(SUM(
                   TIMESTAMPDIFF(MICROSECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) / 1000
               ), 0) AS SIGNED) as totalMillis,
               RANK() OVER (ORDER BY COALESCE(SUM(
                   TIMESTAMPDIFF(MICROSECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) / 1000
               ), 0) DESC) as ranking
        FROM user u 
        LEFT JOIN study_session s ON u.id = s.user_id 
            AND s.start_time <= :periodEnd
            AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        WHERE u.role = 'USER'
        GROUP BY u.id, u.nickname, u.picture, u.department
        ORDER BY COALESCE(SUM(TIMESTAMPDIFF(MICROSECOND,
            GREATEST(s.start_time, :periodStart),
            CASE 
                WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                WHEN s.end_time > :periodEnd THEN :periodEnd
                ELSE s.end_time
            END
        ) / 1000), 0) DESC
        LIMIT 100
""", nativeQuery = true)
    List<PersonalRankingTemp> calculateCurrentPeriodRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now
    );

    /*
    현재 진행중인 기간의 학과별 공부 시간 연산
     */
    @Query(value = """
        SELECT u.id as userId,
               u.nickname as nickname,
               u.picture as imageUrl,
               u.department as department,
               CAST(COALESCE(SUM(
                   TIMESTAMPDIFF(MICROSECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) / 1000
               ), 0) AS SIGNED) as totalMillis,
               RANK() OVER (ORDER BY COALESCE(SUM(
                   TIMESTAMPDIFF(MICROSECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) / 1000
               ), 0) DESC) as ranking
        FROM user u
        LEFT JOIN study_session s ON u.id = s.user_id
            AND s.start_time <= :periodEnd
            AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        WHERE u.role = 'USER' AND u.department = :department
        GROUP BY u.id, u.nickname, u.picture, u.department
        ORDER BY COALESCE(SUM(TIMESTAMPDIFF(MICROSECOND,
            GREATEST(s.start_time, :periodStart),
            CASE
                WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                WHEN s.end_time > :periodEnd THEN :periodEnd
                ELSE s.end_time
            END
        ) / 1000), 0) DESC
        LIMIT 100
""", nativeQuery = true)
    List<PersonalRankingTemp> calculateCurrentPeriodDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now,
            @Param("department") String department
    );

    /*
    랭킹 집계 시 공부 시간
     */
    @Query(value = """
        SELECT u.id as userId, 
               u.nickname as nickname,
               u.picture as imageUrl,
               u.department as department,
               CAST(FLOOR(COALESCE(SUM(
                   TIMESTAMPDIFF(MICROSECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :periodEnd
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   )
               ), 0) / 1000) AS SIGNED) as totalMillis,
               RANK() OVER (ORDER BY FLOOR(COALESCE(SUM(
                   TIMESTAMPDIFF(MICROSECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :periodEnd
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   )
               ), 0) / 1000) DESC) as ranking
        FROM user u 
        LEFT JOIN study_session s ON u.id = s.user_id 
            AND s.start_time <= :periodEnd
            AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        WHERE u.role = 'USER'
        GROUP BY u.id, u.nickname, u.picture, u.department
        ORDER BY FLOOR(COALESCE(SUM(
            TIMESTAMPDIFF(MICROSECOND,
                GREATEST(s.start_time, :periodStart),
                CASE
                    WHEN s.end_time IS NULL THEN :periodEnd
                    WHEN s.end_time > :periodEnd THEN :periodEnd
                    ELSE s.end_time
                END
            )
        ), 0) / 1000) DESC
        LIMIT 100
    """, nativeQuery = true)
    List<PersonalRankingTemp> calculateFinalizedPeriodRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

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
        ),
        dept_rankings AS (
            SELECT
                department,
                CAST(SUM(totalMillis) AS SIGNED) as totalMillis
            FROM (
                SELECT
                    u.department,
                    u.id as userId,
                    COALESCE(SUM(
                        TIMESTAMPDIFF(MICROSECOND,
                            GREATEST(s.start_time, :periodStart),
                            CASE
                                WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                                WHEN s.end_time > :periodEnd THEN :periodEnd
                                ELSE s.end_time
                            END
                        ) / 1000
                    ), 0) as totalMillis,
                    ROW_NUMBER() OVER (
                        PARTITION BY u.department
                        ORDER BY COALESCE(SUM(
                            TIMESTAMPDIFF(MICROSECOND,
                                GREATEST(s.start_time, :periodStart),
                                CASE
                                    WHEN s.end_time IS NULL THEN LEAST(:now, :periodEnd)
                                    WHEN s.end_time > :periodEnd THEN :periodEnd
                                    ELSE s.end_time
                                END
                            ) / 1000
                        ), 0) DESC
                    ) as deptRank
                FROM user u
                LEFT JOIN study_session s ON u.id = s.user_id
                    AND s.start_time <= :periodEnd
                    AND (s.end_time >= :periodStart OR s.end_time IS NULL)
                WHERE u.role = 'USER' AND u.department IS NOT NULL
                GROUP BY u.department, u.id
            ) ranked_users
            WHERE deptRank <= 30
            GROUP BY department
        )
        SELECT d.dept as department,
               COALESCE(dr.totalMillis, 0) as totalMillis,
               RANK() OVER (ORDER BY COALESCE(dr.totalMillis, 0) DESC) as ranking
        FROM all_departments d
        LEFT JOIN dept_rankings dr ON d.dept = dr.department
        ORDER BY COALESCE(dr.totalMillis, 0) DESC
        """, nativeQuery = true)
    List<DepartmentRankingTemp> calculateCurrentDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now);

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
        ),
        dept_rankings AS (
            SELECT
                department,
                CAST(SUM(totalMillis) AS SIGNED) as totalMillis
            FROM (
                SELECT
                    u.department,
                    u.id as userId,
                    COALESCE(SUM(
                        TIMESTAMPDIFF(MICROSECOND,
                            GREATEST(s.start_time, :periodStart),
                            LEAST(s.end_time, :periodEnd)
                        ) / 1000
                    ), 0) as totalMillis,
                    ROW_NUMBER() OVER (
                        PARTITION BY u.department
                        ORDER BY COALESCE(SUM(
                            TIMESTAMPDIFF(MICROSECOND,
                                GREATEST(s.start_time, :periodStart),
                                LEAST(s.end_time, :periodEnd)
                            ) / 1000
                        ), 0) DESC
                    ) as deptRank
                FROM user u
                LEFT JOIN study_session s ON u.id = s.user_id
                    AND s.start_time <= :periodEnd
                    AND s.end_time >= :periodStart
                WHERE u.role = 'USER' AND u.department IS NOT NULL
                GROUP BY u.department, u.id
            ) ranked_users
            WHERE deptRank <= 30
            GROUP BY department
        )
        SELECT d.dept as department,
               COALESCE(dr.totalMillis, 0) as totalMillis,
               RANK() OVER (ORDER BY COALESCE(dr.totalMillis, 0) DESC) as ranking
        FROM all_departments d
        LEFT JOIN dept_rankings dr ON d.dept = dr.department
        ORDER BY COALESCE(dr.totalMillis, 0) DESC
        """, nativeQuery = true)
    List<DepartmentRankingTemp> calculateFinalizedDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );
}
