package com.gpt.geumpumtabackend.study.repository;

import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.statistics.dto.DayMaxFocusStatistics;
import com.gpt.geumpumtabackend.statistics.dto.TwoHourSlotStatistics;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {



    Optional<StudySession> findByIdAndUser_Id(Long id, Long userId);


    // 날짜가 오늘이고, userId와 일치하고, endTime이 null이 아닌 것
    @Query(value = "SELECT COALESCE(SUM(s.total_millis), 0) " +
            "FROM study_session s " +
            "WHERE s.user_id = :userId " +
            "AND s.end_time BETWEEN :startOfDay AND :endOfDay", nativeQuery = true)
    Long sumCompletedStudySessionByUserId(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /*
    현재 진행중인 기간의 공부 시간 연산
     */
    @Query(value = """
        SELECT u.id as userId, 
               u.name as username, 
               SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :now
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) as totalMillis,
               RANK() OVER (ORDER BY SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :now
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) DESC) as ranking
        FROM study_session s 
        JOIN user u ON s.user_id = u.id
        WHERE s.start_time <= :periodEnd
        AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        GROUP BY u.id, u.name
        ORDER BY SUM(TIMESTAMPDIFF(SECOND,
            GREATEST(s.start_time, :periodStart),
            CASE 
                WHEN s.end_time IS NULL THEN :now
                WHEN s.end_time > :periodEnd THEN :periodEnd
                ELSE s.end_time
            END
        ) * 1000) DESC
""", nativeQuery = true)
    List<PersonalRankingTemp> calculateCurrentPeriodRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now
    );




    /*
    랭킹 집계 시 공부 시간
     */
    @Query(value = """
        SELECT u.id as userId, 
               u.name as username, 
               SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :periodEnd
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) as totalMillis,
               RANK() OVER (ORDER BY SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :periodEnd
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) DESC) as ranking
        FROM study_session s 
        JOIN user u ON s.user_id = u.id
        WHERE s.start_time <= :periodEnd
        AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        GROUP BY u.id, u.name
        ORDER BY SUM(
            TIMESTAMPDIFF(SECOND,
                GREATEST(s.start_time, :periodStart),
                CASE
                    WHEN s.end_time IS NULL THEN :periodEnd
                    WHEN s.end_time > :periodEnd THEN :periodEnd
                    ELSE s.end_time
                END
            ) * 1000
        ) DESC
    """, nativeQuery = true)
    List<PersonalRankingTemp> calculateFinalizedPeriodRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Query(value = """
        SELECT u.department as departmentName, 
               SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :now
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) as totalMillis,
               RANK() OVER (ORDER BY SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :now
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) DESC) as ranking
        FROM study_session s 
        JOIN user u ON s.user_id = u.id
        WHERE s.start_time <= :periodEnd AND u.department IS NOT NULL
        AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        GROUP BY u.department
        ORDER BY SUM(TIMESTAMPDIFF(SECOND,
            GREATEST(s.start_time, :periodStart),
            CASE 
                WHEN s.end_time IS NULL THEN :now
                WHEN s.end_time > :periodEnd THEN :periodEnd
                ELSE s.end_time
            END
        ) * 1000) DESC
""", nativeQuery = true)
    List<DepartmentRankingTemp> calculateCurrentDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now);

    @Query(value = """
        SELECT u.department as departmentName, 
               SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :periodEnd
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) as totalMillis,
               RANK() OVER (ORDER BY SUM(
                   TIMESTAMPDIFF(SECOND,
                       GREATEST(s.start_time, :periodStart),
                       CASE
                           WHEN s.end_time IS NULL THEN :periodEnd
                           WHEN s.end_time > :periodEnd THEN :periodEnd
                           ELSE s.end_time
                       END
                   ) * 1000
               ) DESC) as ranking
        FROM study_session s 
        JOIN user u ON s.user_id = u.id
        WHERE s.start_time <= :periodEnd AND u.department IS NOT NULL
        AND (s.end_time >= :periodStart OR s.end_time IS NULL)
        GROUP BY u.department
        ORDER BY SUM(TIMESTAMPDIFF(SECOND,
            GREATEST(s.start_time, :periodStart),
            CASE 
                WHEN s.end_time IS NULL THEN :periodEnd
                WHEN s.end_time > :periodEnd THEN :periodEnd
                ELSE s.end_time
            END
        ) * 1000) DESC
""", nativeQuery = true)
    List<DepartmentRankingTemp> calculateFinalizedDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    // 2시간 단위로 일일 통계를 불러옴
    @Query(
            value = """
        WITH RECURSIVE buckets AS (
            SELECT
                0 AS idx,
                :dayStart AS bucket_start,
                :dayStart + INTERVAL 2 HOUR AS bucket_end
            UNION ALL
            SELECT
                idx + 1,
                bucket_end,
                bucket_end + INTERVAL 2 HOUR
            FROM buckets
            WHERE idx < 11
        )
        SELECT
            DATE_FORMAT(b.bucket_start, '%H:%i') AS slotStart,
            DATE_FORMAT(b.bucket_end,   '%H:%i') AS slotEnd,
            COALESCE(SUM(
                GREATEST(
                  0,
                  TIMESTAMPDIFF(
                    SECOND,
                    GREATEST(s.start_time, b.bucket_start),
                    LEAST(s.end_time,   b.bucket_end)
                  )
                )
              ), 0) AS secondsStudied
        FROM buckets b
        LEFT JOIN study_session s
          ON s.user_id    = :userId
         AND s.start_time <  b.bucket_end
         AND s.end_time   >  b.bucket_start
        GROUP BY b.idx, b.bucket_start, b.bucket_end
        ORDER BY b.idx
        """,
            nativeQuery = true
    )
    List<TwoHourSlotStatistics> getTwoHourSlotStats(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd,
            @Param("userId") Long userId
    );


    @Query(value = """
        SELECT
          CAST(
            COALESCE(
              MAX(
                GREATEST(
                  0,
                  TIMESTAMPDIFF(
                    SECOND,
                    GREATEST(s.start_time, :dayStart),
                    LEAST(s.end_time,   :dayEnd)
                  )
                )
              ), 0
            ) AS SIGNED
          ) AS maxFocusSeconds
        FROM study_session s
        WHERE s.user_id    = :userId
          AND s.start_time < :dayEnd
          AND s.end_time   > :dayStart
""", nativeQuery = true)
    DayMaxFocusStatistics findDayMaxFocus(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd,
            @Param("userId")   Long userId
    );
}
