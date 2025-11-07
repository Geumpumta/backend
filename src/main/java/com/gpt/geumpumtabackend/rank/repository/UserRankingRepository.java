package com.gpt.geumpumtabackend.rank.repository;

import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.domain.UserRanking;
import com.gpt.geumpumtabackend.rank.dto.UserRankingTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRankingRepository extends JpaRepository<UserRanking, Long> {

   /*
   끝난 일간 랭킹
    */
    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.UserRankingTemp(
                    ur.user.id,
                    ur.user.name,
                    ur.totalMillis,
                    ur.rank)
        FROM UserRanking ur WHERE ur.calculatedAt =:date
         AND ur.rankingType = :rankingType
        ORDER BY ur.rank ASC
""")
    List<UserRankingTemp> getFinishedPersonalRanking(@Param("date") LocalDateTime period,  @Param("rankingType") RankingType rankingType);

}
