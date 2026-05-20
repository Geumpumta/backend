package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.domain.UserRanking;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.user.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRankingRepository extends JpaRepository<UserRanking, Long> {

    long countByRankingTypeAndCalculatedAt(RankingType rankingType, LocalDateTime calculatedAt);

   /*
   끝난 일간 랭킹
    */
    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp(
                    ur.user.id,
                    ur.user.nickname,
                    ur.user.picture,
                    ur.user.department,
                    ur.totalMillis,
                    ur.rank)
        FROM UserRanking ur
        WHERE DATE(ur.calculatedAt) = DATE(:date)
         AND ur.rankingType = :rankingType
        ORDER BY ur.rank ASC
""")
    List<PersonalRankingTemp> getFinishedPersonalRanking(@Param("date") LocalDateTime period, @Param("rankingType") RankingType rankingType);


    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp(
            ur.user.id,
            ur.user.nickname,
            ur.user.picture,
            ur.user.department,
            SUM(ur.totalMillis),
            0L
        )
        FROM UserRanking ur
        WHERE ur.rankingType = 'MONTHLY'
            AND ur.calculatedAt >= :seasonStart
            AND ur.calculatedAt < :currentMonthStart
        GROUP BY ur.user.id, ur.user.nickname, ur.user.picture, ur.user.department
        """)
    List<PersonalRankingTemp> calculateSeasonRankingFromMonthlyRankings(
        @Param("seasonStart") LocalDateTime seasonStart,
        @Param("currentMonthStart") LocalDateTime currentMonthStart
    );


    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp(
            ur.user.id,
            ur.user.nickname,
            ur.user.picture,
            ur.user.department,
            SUM(ur.totalMillis),
            0L
        )
        FROM UserRanking ur
        WHERE ur.rankingType = 'DAILY'
            AND ur.calculatedAt >= :currentMonthStart
            AND ur.calculatedAt < :today
        GROUP BY ur.user.id, ur.user.nickname, ur.user.picture, ur.user.department
        """)
    List<PersonalRankingTemp> calculateCurrentMonthRankingFromDailyRankings(
        @Param("currentMonthStart") LocalDateTime currentMonthStart,
        @Param("today") LocalDateTime today
    );

    /**
     * 학과별 - 완료된 월 월간 랭킹 합산
     */
    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp(
            ur.user.id,
            ur.user.nickname,
            ur.user.picture,
            ur.user.department,
            SUM(ur.totalMillis),
            0L
        )
        FROM UserRanking ur
        WHERE ur.rankingType = 'MONTHLY'
            AND ur.calculatedAt >= :seasonStart
            AND ur.calculatedAt < :currentMonthStart
            AND ur.user.department = :department
        GROUP BY ur.user.id, ur.user.nickname, ur.user.picture, ur.user.department
        """)
    List<PersonalRankingTemp> calculateSeasonDepartmentRankingFromMonthlyRankings(
        @Param("seasonStart") LocalDateTime seasonStart,
        @Param("currentMonthStart") LocalDateTime currentMonthStart,
        @Param("department") Department department
    );

    /**
     * 학과별 - 현재 월 일간 랭킹 합산
     */
    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp(
            ur.user.id,
            ur.user.nickname,
            ur.user.picture,
            ur.user.department,
            SUM(ur.totalMillis),
            0L
        )
        FROM UserRanking ur
        WHERE ur.rankingType = 'DAILY'
            AND ur.calculatedAt >= :currentMonthStart
            AND ur.calculatedAt < :today
            AND ur.user.department = :department
        GROUP BY ur.user.id, ur.user.nickname, ur.user.picture, ur.user.department
        """)
    List<PersonalRankingTemp> calculateCurrentMonthDepartmentRankingFromDailyRankings(
        @Param("currentMonthStart") LocalDateTime currentMonthStart,
        @Param("today") LocalDateTime today,
        @Param("department") Department department
    );
}
