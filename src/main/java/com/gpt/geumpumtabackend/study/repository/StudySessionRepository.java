package com.gpt.geumpumtabackend.study.repository;

import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.statistics.dto.*;
import com.gpt.geumpumtabackend.study.domain.StudySession;
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
        SELECT 
            department,
            CAST(SUM(totalMillis) AS SIGNED) as totalMillis,
            RANK() OVER (ORDER BY SUM(totalMillis) DESC) as ranking
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
        ORDER BY SUM(totalMillis) DESC
        """, nativeQuery = true)
    List<DepartmentRankingTemp> calculateCurrentDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now);

    @Query(value = """
        SELECT 
            department,
            CAST(SUM(totalMillis) AS SIGNED) as totalMillis,
            RANK() OVER (ORDER BY SUM(totalMillis) DESC) as ranking
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
        ORDER BY SUM(totalMillis) DESC
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
    WITH clipped AS (
      SELECT GREATEST(
               0,
               TIMESTAMPDIFF(
                 SECOND,
                 GREATEST(s.start_time, :dayStart),
                 LEAST(s.end_time, :dayEnd)
               )
             ) AS overlap_sec
      FROM study_session s
      WHERE s.user_id    = :userId
        AND s.start_time < :dayEnd
        AND s.end_time   > :dayStart
    )
    SELECT
      CAST(COALESCE(SUM(c.overlap_sec), 0) AS SIGNED) AS totalStudySeconds,
      CAST(COALESCE(MAX(c.overlap_sec), 0) AS SIGNED) AS maxFocusSeconds
    FROM clipped c
    """, nativeQuery = true)
    DayMaxFocusAndFullTimeStatistics getDayMaxFocusAndFullTime(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd,
            @Param("userId")   Long userId
    );

    @Query(value = """
        WITH RECURSIVE days AS (
          SELECT 0 AS day_idx,
                 :weekStart AS day_start,
                 DATE_ADD(:weekStart, INTERVAL 1 DAY) AS day_end
          UNION ALL
          SELECT day_idx + 1,
                 DATE_ADD(:weekStart, INTERVAL day_idx + 1 DAY),
                 DATE_ADD(:weekStart, INTERVAL day_idx + 2 DAY)
          FROM days
          WHERE day_idx < 6
        ),
        per_day AS (
          SELECT
            d.day_idx,
            CAST(
              COALESCE(
                SUM(
                  GREATEST(
                    0,
                    TIMESTAMPDIFF(
                      SECOND,
                      GREATEST(s.start_time, d.day_start),
                      LEAST(COALESCE(s.end_time, d.day_end), d.day_end)
                    )
                  )
                ),
                0
              ) AS SIGNED
            ) AS day_seconds
          FROM days d
          LEFT JOIN study_session s
            ON s.user_id    = :userId
           AND s.start_time < d.day_end
           AND s.end_time   > d.day_start
          GROUP BY d.day_idx
        ),
        flags AS (
          SELECT
            day_idx,
            day_seconds,
            CASE WHEN day_seconds > 0 THEN 1 ELSE 0 END AS has_study
          FROM per_day
        ),
        breaks AS (
          SELECT
            day_idx,
            day_seconds,
            has_study,
            SUM(CASE WHEN has_study = 0 THEN 1 ELSE 0 END)
              OVER (ORDER BY day_idx) AS zero_grp
          FROM flags
        ),
        streaks AS (
          SELECT zero_grp, COUNT(*) AS streak_len
          FROM breaks
          WHERE has_study = 1
          GROUP BY zero_grp
        )
        SELECT
          /* 주간 총 공부시간(초) */
          (SELECT CAST(COALESCE(SUM(day_seconds), 0) AS SIGNED) FROM per_day) AS totalWeekSeconds,
          /* 주간 최장 연속 공부일수 */
          COALESCE((SELECT MAX(streak_len) FROM streaks), 0)                  AS maxConsecutiveStudyDays,
          /* 7일 평균(초) — 소수점 버림 */
          CAST(((SELECT COALESCE(SUM(day_seconds), 0) FROM per_day) / 7) AS SIGNED) AS averageDailySeconds
        """, nativeQuery = true)
    WeeklyStatistics getWeeklyStatistics(
            @Param("weekStart") LocalDateTime weekStart,
            @Param("userId") Long userId
    );

    @Query(value = """
        WITH RECURSIVE
        bounds AS (
          SELECT
            :monthStart AS start_at,
            DATE_ADD(LAST_DAY(:monthStart), INTERVAL 1 DAY) AS end_at,
            TIMESTAMPDIFF(
              DAY, :monthStart,
              DATE_ADD(LAST_DAY(:monthStart), INTERVAL 1 DAY)
            ) AS days_cnt
        ),
        /* 월 전체 일자를 day_idx=0..(days_cnt-1)로 생성 */
        days AS (
          SELECT
            0 AS day_idx,
            b.start_at AS day_start,
            LEAST(DATE_ADD(b.start_at, INTERVAL 1 DAY), b.end_at) AS day_end
          FROM bounds b
          UNION ALL
          SELECT
            d.day_idx + 1,
            DATE_ADD(d.day_start, INTERVAL 1 DAY),
            LEAST(DATE_ADD(d.day_start, INTERVAL 2 DAY), b.end_at)
          FROM days d
          JOIN bounds b
            ON d.day_end < b.end_at
        ),
        /* 일자별 공부 총합(초) */
        per_day AS (
          SELECT
            d.day_idx,
            CAST(
              COALESCE(
                SUM(
                  GREATEST(
                    0,
                    TIMESTAMPDIFF(
                      SECOND,
                      GREATEST(s.start_time, d.day_start),
                      LEAST(COALESCE(s.end_time, d.day_end), d.day_end)
                    )
                  )
                ), 0
              ) AS SIGNED
            ) AS day_seconds
          FROM days d
          LEFT JOIN study_session s
            ON s.user_id    = :userId
           AND s.start_time < d.day_end
           AND s.end_time   > d.day_start
          GROUP BY d.day_idx
        ),
        flags AS (
          SELECT
            day_idx,
            day_seconds,
            CASE WHEN day_seconds > 0 THEN 1 ELSE 0 END AS has_study
          FROM per_day
        ),
        breaks AS (
          /* 0(공부 안 한 날)을 경계로 그룹을 나눠 연속 구간 식별 */
          SELECT
            day_idx,
            day_seconds,
            has_study,
            SUM(CASE WHEN has_study = 0 THEN 1 ELSE 0 END)
              OVER (ORDER BY day_idx) AS zero_grp
          FROM flags
        ),
        streaks AS (
          SELECT zero_grp, COUNT(*) AS streak_len
          FROM breaks
          WHERE has_study = 1
          GROUP BY zero_grp
        )
        SELECT
          /* 총 공부시간(초) */
          CAST(COALESCE((SELECT SUM(day_seconds) FROM per_day), 0) AS SIGNED) AS totalMonthSeconds,
          /* 월 일수로 나눈 일일 평균(초; 소수 버림) */
          CAST( (COALESCE((SELECT SUM(day_seconds) FROM per_day), 0)
                / NULLIF((SELECT days_cnt FROM bounds), 0)) AS SIGNED)        AS averageDailySeconds,
          /* 최장 연속 공부 일수 */
          COALESCE((SELECT MAX(streak_len) FROM streaks), 0)                  AS maxConsecutiveStudyDays,
          /* 이번 달 공부 일수(>0초) */
          (SELECT COUNT(*) FROM per_day WHERE day_seconds > 0)                AS studiedDays
        """, nativeQuery = true)
    MonthlyStatistics getMonthlyStatistics(
            @Param("monthStart") LocalDateTime monthStart, // 해당 월 1일 00:00
            @Param("userId") Long userId
    );


    @Query(value = """
    WITH RECURSIVE
    bounds AS (
      SELECT DATE(:monthStart) AS start_at,
             DATE(:monthEnd)   AS end_at_exclusive
    ),
    days AS (
      SELECT b.start_at AS day_date,
             CAST(b.start_at AS DATETIME) AS day_start,
             CAST(DATE_ADD(b.start_at, INTERVAL 1 DAY) AS DATETIME) AS day_end
      FROM bounds b
      UNION ALL
      SELECT DATE_ADD(d.day_date, INTERVAL 1 DAY),
             DATE_ADD(d.day_start, INTERVAL 1 DAY),
             DATE_ADD(d.day_end,   INTERVAL 1 DAY)
      FROM days d
      JOIN bounds b ON d.day_date < b.end_at_exclusive
    ),
    sessions_in_window AS (
      SELECT s.user_id, s.start_time, s.end_time
      FROM study_session s
      JOIN bounds b
        ON s.user_id = :userId
       AND s.end_time   > b.start_at
       AND s.start_time < b.end_at_exclusive
       AND s.end_time IS NOT NULL
    ),
    day_overlap AS (
      SELECT d.day_date,
             GREATEST(s.start_time, d.day_start) AS seg_start,
             LEAST(s.end_time,   d.day_end)      AS seg_end
      FROM days d
      JOIN sessions_in_window s
        ON s.end_time   > d.day_start
       AND s.start_time < d.day_end
    ),
    daily_sec AS (
      SELECT day_date AS date,
             GREATEST(SUM(GREATEST(TIMESTAMPDIFF(SECOND, seg_start, seg_end), 0)), 0) AS total_seconds
      FROM day_overlap
      GROUP BY day_date
    ),
    daily_full AS (
      SELECT d.day_date AS date,
             COALESCE(ds.total_seconds, 0) AS total_seconds
      FROM days d
      LEFT JOIN daily_sec ds ON ds.date = d.day_date
    ),
    leveled AS (
      SELECT date,
             CASE
               WHEN total_seconds = 0 THEN 0
               ELSE NTILE(4) OVER (
                      PARTITION BY YEAR(date), MONTH(date)
                      ORDER BY total_seconds
                    )
             END AS level
      FROM daily_full
    )
    SELECT
      DATE_FORMAT(date, '%Y-%m-%d')              AS date,
      CAST(level AS UNSIGNED)                    AS level
    FROM leveled
    ORDER BY date
    """, nativeQuery = true)
    List<GrassStatistics> getGrassStatistics(
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd,
            @Param("userId") Long userId
    );
}
