package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.redis.RedisRealtimeRankingReader;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentRankService {

    private final DepartmentRankingRepository departmentRankingRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;

    // 현재 진행 중 학과 랭킹은 Redis 조회 모델을 우선 조회하고, Redis가 비어 있거나 장애가 나면 기존 MySQL 쿼리로 대체 조회한다.
    @Autowired(required = false)
    private RedisRealtimeRankingReader redisRankingReader;

    /*
    현재 진행중인 학과 랭킹 일간 조회
     */
    public DepartmentRankingResponse getCurrentDailyDepartmentRanking(Long userId){
        if (redisRankingReader != null) {
            try {
                // Redis 키가 없으면 Optional.empty()가 반환되고 아래 DB 대체 조회 경로를 탄다.
                return redisRankingReader.departmentDaily(userId)
                        .orElseGet(() -> getCurrentDailyDepartmentRankingFromDatabase(userId));
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[REDIS_RANKING] Falling back to DB for current daily department ranking. userId={}", userId, e);
            }
        }

        return getCurrentDailyDepartmentRankingFromDatabase(userId);
    }

    private DepartmentRankingResponse getCurrentDailyDepartmentRankingFromDatabase(Long userId){
        LocalDate today = LocalDate.now();
        LocalDateTime startDay = today.atStartOfDay();
        LocalDateTime endDay = today.atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<DepartmentRankingTemp> departmentRankingList = studySessionRepository.calculateCurrentDepartmentRanking(startDay, endDay, nowTime);
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }

    /*
    완료된 학과 랭킹 일간 조회
     */
    public DepartmentRankingResponse getCompletedDailyDepartmentRanking(Long userId, LocalDateTime startDay){
        List<DepartmentRankingTemp> departmentRankingList = departmentRankingRepository.getFinishedDepartmentRanking(startDay, RankingType.DAILY.name());
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }

     /*
    현재 진행중인 학과 랭킹 주간 조회
     */
    public DepartmentRankingResponse getCurrentWeeklyDepartmentRanking(Long userId){
        if (redisRankingReader != null) {
            try {
                // Redis 키가 없으면 Optional.empty()가 반환되고 아래 DB 대체 조회 경로를 탄다.
                return redisRankingReader.departmentWeekly(userId)
                        .orElseGet(() -> getCurrentWeeklyDepartmentRankingFromDatabase(userId));
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[REDIS_RANKING] Falling back to DB for current weekly department ranking. userId={}", userId, e);
            }
        }

        return getCurrentWeeklyDepartmentRankingFromDatabase(userId);
    }

    private DepartmentRankingResponse getCurrentWeeklyDepartmentRankingFromDatabase(Long userId){
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<DepartmentRankingTemp> departmentRankingList = studySessionRepository.calculateCurrentDepartmentRanking(weekStart, weekEnd, nowTime);
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }

    /*
    완료된 학과 랭킹 주간 조회
     */
    public DepartmentRankingResponse getCompletedWeeklyDepartmentRanking(Long userId, LocalDateTime weekFirstDay){
        List<DepartmentRankingTemp> departmentRankingList = departmentRankingRepository.getFinishedDepartmentRanking(weekFirstDay, RankingType.WEEKLY.name());
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }


     /*
    현재 진행중인 학과 랭킹 월간 조회
     */
     public DepartmentRankingResponse getCurrentMonthlyDepartmentRanking(Long userId){
         if (redisRankingReader != null) {
             try {
                 // Redis 키가 없으면 Optional.empty()가 반환되고 아래 DB 대체 조회 경로를 탄다.
                 return redisRankingReader.departmentMonthly(userId)
                         .orElseGet(() -> getCurrentMonthlyDepartmentRankingFromDatabase(userId));
             } catch (BusinessException e) {
                 throw e;
             } catch (Exception e) {
                 log.warn("[REDIS_RANKING] Falling back to DB for current monthly department ranking. userId={}", userId, e);
             }
         }

         return getCurrentMonthlyDepartmentRankingFromDatabase(userId);
     }

     private DepartmentRankingResponse getCurrentMonthlyDepartmentRankingFromDatabase(Long userId){
         LocalDate today = LocalDate.now();
         LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
         LocalDateTime endMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);
         LocalDateTime nowTime = LocalDateTime.now();
         List<DepartmentRankingTemp> departmentRankingList = studySessionRepository.calculateCurrentDepartmentRanking(startMonth, endMonth, nowTime);
         return buildDepartmentRankingResponse(departmentRankingList, userId);
     }


    /*
    완료된 학과 랭킹 월간 조회
     */
    public DepartmentRankingResponse getCompletedMonthlyDepartmentRanking(Long userId, LocalDateTime monthFirstDay){
        List<DepartmentRankingTemp> departmentRankingList = departmentRankingRepository.getFinishedDepartmentRanking(monthFirstDay, RankingType.MONTHLY.name());
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }

    private DepartmentRankingResponse buildDepartmentRankingResponse(List<DepartmentRankingTemp> departmentRankingList, Long userId) {
        DepartmentRankingEntryResponse myRanking = null;
        List<DepartmentRankingEntryResponse> topRankings = new ArrayList<>();
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));

        for (DepartmentRankingTemp temp : departmentRankingList) {
            DepartmentRankingEntryResponse entry = DepartmentRankingEntryResponse.of(temp);
            topRankings.add(entry);

            // 사용자의 학과 찾기 (myRanking용)
            if(user.getDepartment() != null && user.getDepartment().getKoreanName().equals(temp.getDepartmentName())){
                myRanking = entry;
            }
        }

        // 사용자의 학과를 찾지 못한 경우 0초, 마지막 순위로 설정
        if (myRanking == null && user.getDepartment() != null) {
            myRanking = new DepartmentRankingEntryResponse(
                user.getDepartment().getKoreanName(),
                0L,
                (long) topRankings.size() + 1
            );
        }

        return new DepartmentRankingResponse(topRankings, myRanking);
    }
}

