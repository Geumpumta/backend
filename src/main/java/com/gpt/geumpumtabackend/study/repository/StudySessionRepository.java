package com.gpt.geumpumtabackend.study.repository;

import com.gpt.geumpumtabackend.rank.domain.DepartmentRanking;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.UserRankingTemp;
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
    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.UserRankingTemp(
            s.user.id,
            s.user.name,
            SUM(
                TIMESTAMPDIFF('SECOND',
                    GREATEST(s.startTime, :periodStart),
                    CASE
                        WHEN s.endTime IS NULL THEN :now
                        WHEN s.endTime > :periodEnd THEN :periodEnd
                        ELSE s.endTime
                    END
                ) * 1000
            ), 
            RANK() OVER (ORDER BY SUM(                                                                                                                                                                     
              TIMESTAMPDIFF('SECOND',                                                                                                                                                                      
                  GREATEST(s.startTime, :periodStart),                                                                                                                                                      
                  CASE                                                                                                                                                                                   
                      WHEN s.endTime IS NULL THEN :now                                                                                                                                              
                      WHEN s.endTime > :periodEnd THEN :periodEnd                                                                                                                                              
                      ELSE s.endTime                                                                                                                                                                     
                  END                                                                                                                                                                                    
              ) * 1000                                                                                                                                                                                   
          ) DESC)  
        )
        FROM StudySession s 
        WHERE s.startTime <= :periodEnd
        AND (s.endTime >= :periodStart OR s.endTime IS NULL)
        GROUP BY s.user.id
        ORDER BY SUM(TIMESTAMPDIFF('SECOND',
            GREATEST(s.startTime, :periodStart),
            CASE 
                WHEN s.endTime IS NULL THEN :now
                WHEN s.endTime > :periodEnd THEN :periodEnd
                ELSE s.endTime
            END
        ) * 1000
    ) DESC
""")
          List<UserRankingTemp> calculateCurrentPeriodRanking(
                  @Param("periodStart") LocalDateTime periodStart,
                  @Param("periodEnd") LocalDateTime periodEnd,
                  @Param("now") LocalDateTime now
          );




    /*
    랭킹 집계 시 공부 시간
     */
    @Query("""                                                                                                                                                                                             
      SELECT new com.gpt.geumpumtabackend.rank.dto.UserRankingTemp(
          s.user.id,
          s.user.name,
          SUM(
              TIMESTAMPDIFF('SECOND',
                  GREATEST(s.startTime, :periodStart),
                  CASE                                                                       
                      WHEN s.endTime IS NULL THEN :periodEnd                                                                                                                                                
                      WHEN s.endTime > :periodEnd THEN :periodEnd                                                                                                                                              
                      ELSE s.endTime                                                                                                                                                                     
                  END                                                                                                                                                                                    
              ) * 1000                                                                                                                                                                                   
          ),                                                                                                                                                                                             
          RANK() OVER (ORDER BY SUM(                                                                                                                                                                     
              TIMESTAMPDIFF('SECOND',                                                                                                                                                                      
                  GREATEST(s.startTime, :periodStart),                                                                                                                                                      
                  CASE                                                                                                                                                                                   
                      WHEN s.endTime IS NULL THEN :periodEnd                                                                                                                                                
                      WHEN s.endTime > :periodEnd THEN :periodEnd                                                                                                                                              
                      ELSE s.endTime                                                                                                                                                                     
                  END                                                                                                                                                                                    
              ) * 1000                                                                                                                                                                                   
          ) DESC)                                                                                                                                                                                        
      )                                                                                                                                                                                                  
      FROM StudySession s                                                                                                                                                                                
      WHERE s.startTime <= :periodEnd                                                                                                                                                                       
      AND (s.endTime > :periodStart OR s.endTime IS NULL)                                                                                                                                                   
      GROUP BY s.user.id                                                                                                                                                                                 
      ORDER BY SUM(                                                                                                                                                                                      
          TIMESTAMPDIFF('SECOND',                                                                                                                                                                          
              GREATEST(s.startTime, :periodStart),                                                                                                                                                          
              CASE                                                                                                                                                                                       
                  WHEN s.endTime IS NULL THEN :periodEnd                                                                                                                                                    
                  WHEN s.endTime > :periodEnd THEN :periodEnd                                                                                                                                                  
                  ELSE s.endTime                                                                                                                                                                         
              END                                                                                                                                                                                        
          ) * 1000                                                                                                                                                                                       
      ) DESC                                                                                                                                                                                             
  """)
    List<UserRankingTemp> calculateFinalizedPeriodRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp(
            s.user.department,
            SUM(
                TIMESTAMPDIFF('SECOND',
                    GREATEST(s.startTime, :periodStart),
                    CASE
                        WHEN s.endTime IS NULL THEN :now
                        ELSE s.endTime
                    END
                ) * 1000
            ),
            RANK() OVER (ORDER BY SUM(                                                                                                                                                                     
              TIMESTAMPDIFF('SECOND',                                                                                                                                                                      
                  GREATEST(s.startTime, :periodStart),                                                                                                                                                      
                  CASE                                                                                                                                                                                   
                      WHEN s.endTime IS NULL THEN :periodEnd                                                                                                                                                
                      WHEN s.endTime > :periodEnd THEN :periodEnd                                                                                                                                              
                      ELSE s.endTime                                                                                                                                                                     
                  END                                                                                                                                                                                    
              ) * 1000                                                                                                                                                                                   
          ) DESC)  
        )
         FROM StudySession s 
        WHERE s.startTime <= :periodEnd AND s.user.department IS NOT NULL
        AND (s.endTime >= :periodStart OR s.endTime IS NULL)
        GROUP BY s.user.department
        ORDER BY SUM(TIMESTAMPDIFF('SECOND',
            GREATEST(s.startTime, :periodStart),
            CASE 
                WHEN s.endTime IS NULL THEN :periodEnd
                WHEN s.endTime > :periodEnd THEN :periodEnd
                ELSE s.endTime
            END
        ) * 1000
    ) DESC
""")
    List<DepartmentRankingTemp> calculateCurrentDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd,
            @Param("now") LocalDateTime now);

    @Query("""
        SELECT new com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp(
            s.user.department,
            SUM(
                TIMESTAMPDIFF('SECOND',
                    GREATEST(s.startTime, :periodStart),
                    CASE
                        WHEN s.endTime IS NULL THEN :periodEnd
                        WHEN s.endTime > :periodEnd THEN :periodEnd
                        ELSE s.endTime
                    END
                ) * 1000
            ), 
            RANK() OVER (ORDER BY SUM(                                                                                                                                                                     
              TIMESTAMPDIFF('SECOND',                                                                                                                                                                      
                  GREATEST(s.startTime, :periodStart),                                                                                                                                                      
                  CASE                                                                                                                                                                                   
                      WHEN s.endTime IS NULL THEN :periodEnd                                                                                                                                                
                      WHEN s.endTime > :periodEnd THEN :periodEnd                                                                                                                                              
                      ELSE s.endTime                                                                                                                                                                     
                  END                                                                                                                                                                                    
              ) * 1000                                                                                                                                                                                   
          ) DESC)  
        )
         FROM StudySession s 
        WHERE s.startTime <= :periodEnd AND s.user.department IS NOT NULL
        AND (s.endTime >= :periodStart OR s.endTime IS NULL)
        GROUP BY s.user.department
        ORDER BY SUM(TIMESTAMPDIFF('SECOND',
            GREATEST(s.startTime, :periodStart),
            CASE 
                WHEN s.endTime IS NULL THEN :periodEnd
                WHEN s.endTime > :periodEnd THEN :periodEnd
                ELSE s.endTime
            END
        ) * 1000
    ) DESC
""")
    List<DepartmentRankingTemp> calculateFinalizedDepartmentRanking(
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );




}
