package com.gpt.geumpumtabackend.statistics.repository;

import com.gpt.geumpumtabackend.statistics.dto.*;
import com.gpt.geumpumtabackend.study.domain.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsRepository extends JpaRepository<StudySession, Long> {
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
                    MICROSECOND,
                    GREATEST(s.start_time, b.bucket_start),
                    LEAST(s.end_time,   b.bucket_end)
                  ) / 1000
                )
              ), 0) AS millisecondsStudied
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
                 MICROSECOND,
                 GREATEST(s.start_time, :dayStart),
                 LEAST(s.end_time, :dayEnd)
               ) / 1000
             ) AS overlap_ms
      FROM study_session s
      WHERE s.user_id    = :userId
        AND s.start_time < :dayEnd
        AND s.end_time   > :dayStart
    )
    SELECT
      CAST(COALESCE(SUM(c.overlap_ms), 0) AS SIGNED) AS totalStudyMillis,
      CAST(COALESCE(MAX(c.overlap_ms), 0) AS SIGNED) AS maxFocusMillis
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
                      MICROSECOND,
                      GREATEST(s.start_time, d.day_start),
                      LEAST(COALESCE(s.end_time, d.day_end), d.day_end)
                    ) / 1000
                  )
                ),
                0
              ) AS SIGNED
            ) AS day_millis
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
            day_millis,
            CASE WHEN day_millis > 0 THEN 1 ELSE 0 END AS has_study
          FROM per_day
        ),
        breaks AS (
          SELECT
            day_idx,
            day_millis,
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
          /* 주간 총 공부시간(ms) */
          (SELECT CAST(COALESCE(SUM(day_millis), 0) AS SIGNED) FROM per_day) AS totalWeekMillis,
          /* 주간 최장 연속 공부일수 */
          COALESCE((SELECT MAX(streak_len) FROM streaks), 0)                  AS maxConsecutiveStudyDays,
          /* 7일 평균(ms) — 소수점 버림 */
          CAST(((SELECT COALESCE(SUM(day_millis), 0) FROM per_day) / 7) AS SIGNED) AS averageDailyMillis
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

    /* (개선) 이번 달과 겹치는 "완료된" 세션만 먼저 선필터링 */
    filtered_sessions AS (
      SELECT s.user_id, s.start_time, s.end_time
      FROM study_session s
      JOIN bounds b
        ON s.user_id = :userId
       AND s.end_time IS NOT NULL                 /* 진행 중 세션 제외 (의도 유지) */
       AND s.start_time < b.end_at                /* 달 끝보다 먼저 시작 */
       AND s.end_time   > b.start_at              /* 달 시작보다 나중에 끝 */
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

    /* 일자별 공부 총합(ms) */
    per_day AS (
      SELECT
        d.day_idx,
        CAST(
          COALESCE(
            SUM(
              GREATEST(
                0,
                TIMESTAMPDIFF(
                  MICROSECOND,
                  GREATEST(s.start_time, d.day_start),
                  LEAST(s.end_time, d.day_end)
                ) / 1000
              )
            ), 0
          ) AS SIGNED
        ) AS day_millis
      FROM days d
      LEFT JOIN filtered_sessions s
        ON s.start_time < d.day_end
       AND s.end_time   > d.day_start
      GROUP BY d.day_idx
    ),

    flags AS (
      SELECT
        day_idx,
        day_millis,
        CASE WHEN day_millis > 0 THEN 1 ELSE 0 END AS has_study
      FROM per_day
    ),

    breaks AS (
      /* 0(공부 안 한 날)을 경계로 그룹을 나눠 연속 구간 식별 */
      SELECT
        day_idx,
        day_millis,
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
      /* 총 공부시간(ms) */
      CAST(COALESCE((SELECT SUM(day_millis) FROM per_day), 0) AS SIGNED) AS totalMonthMillis,

      /* 월 일수로 나눈 일일 평균(ms; 소수 버림) */
      CAST( (COALESCE((SELECT SUM(day_millis) FROM per_day), 0)
            / NULLIF((SELECT days_cnt FROM bounds), 0)) AS SIGNED)        AS averageDailyMillis,

      /* 최장 연속 공부 일수 */
      COALESCE((SELECT MAX(streak_len) FROM streaks), 0)                  AS maxConsecutiveStudyDays,

      /* 이번 달 공부 일수(>0ms) */
      (SELECT COUNT(*) FROM per_day WHERE day_millis > 0)                 AS studiedDays
    """, nativeQuery = true)
    MonthlyStatistics getMonthlyStatistics(
            @Param("monthStart") LocalDateTime monthStart, // 해당 월 1일 00:00
            @Param("userId") Long userId
    );

    @Query(value = """
        WITH RECURSIVE streak(day_date, day_millis) AS (
            SELECT DATE(:today) AS day_date,
                   CAST(COALESCE((
                       SELECT SUM(GREATEST(
                           0,
                           TIMESTAMPDIFF(
                               MICROSECOND,
                               GREATEST(s.start_time, DATE(:today)),
                               LEAST(COALESCE(s.end_time, DATE(:today) + INTERVAL 1 DAY), DATE(:today) + INTERVAL 1 DAY)
                           ) / 1000
                       ))
                       FROM study_session s
                       WHERE s.user_id = :userId
                         AND s.start_time < DATE(:today) + INTERVAL 1 DAY
                         AND s.end_time > DATE(:today)
                   ), 0) AS SIGNED) AS day_millis
            UNION ALL
            SELECT DATE_SUB(streak.day_date, INTERVAL 1 DAY) AS day_date,
                   CAST(COALESCE((
                       SELECT SUM(GREATEST(
                           0,
                           TIMESTAMPDIFF(
                               MICROSECOND,
                               GREATEST(s.start_time, DATE_SUB(streak.day_date, INTERVAL 1 DAY)),
                               LEAST(COALESCE(s.end_time, DATE_SUB(streak.day_date, INTERVAL 1 DAY) + INTERVAL 1 DAY),
                                     DATE_SUB(streak.day_date, INTERVAL 1 DAY) + INTERVAL 1 DAY)
                           ) / 1000
                       ))
                       FROM study_session s
                       WHERE s.user_id = :userId
                         AND s.start_time < DATE_SUB(streak.day_date, INTERVAL 1 DAY) + INTERVAL 1 DAY
                         AND s.end_time > DATE_SUB(streak.day_date, INTERVAL 1 DAY)
                   ), 0) AS SIGNED) AS day_millis
            FROM streak
            WHERE streak.day_millis >= :thresholdMillis
        )
        SELECT COUNT(*)
        FROM streak
        WHERE day_millis >= :thresholdMillis
        """, nativeQuery = true)
    Integer countCurrentConsecutiveStudyDays(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("thresholdMillis") Long thresholdMillis
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
      JOIN bounds b ON d.day_date < DATE_SUB(b.end_at_exclusive, INTERVAL 1 DAY)
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
    daily_ms AS (
      SELECT day_date AS date,
             GREATEST(SUM(GREATEST(TIMESTAMPDIFF(MICROSECOND, seg_start, seg_end), 0)) / 1000, 0) AS total_millis
      FROM day_overlap
      GROUP BY day_date
    ),
    daily_full AS (
      SELECT d.day_date AS date,
             COALESCE(ds.total_millis, 0) AS total_millis
      FROM days d
      LEFT JOIN daily_ms ds ON ds.date = d.day_date
    ),
    leveled AS (
      SELECT date,
             CASE
               WHEN total_millis = 0 THEN 0
               ELSE NTILE(4) OVER (
                      PARTITION BY YEAR(date), MONTH(date)
                      ORDER BY total_millis
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
