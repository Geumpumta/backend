package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.repository.DepartmentRankingRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentRankService {

    private final DepartmentRankingRepository departmentRankingRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;

    /*
    현재 진행중인 학과 랭킹 일간 조회
     */
    public DepartmentRankingResponse getCurrentDailyDepartmentRanking(Long userId){
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
        List<DepartmentRankingTemp> departmentRankingList = departmentRankingRepository.getFinishedDepartmentRanking(startDay, RankingType.DAILY);
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }

     /*
    현재 진행중인 학과 랭킹 주간 조회
     */
    public DepartmentRankingResponse getCurrentWeeklyDepartmentRanking(Long userId){
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
        List<DepartmentRankingTemp> departmentRankingList = departmentRankingRepository.getFinishedDepartmentRanking(weekFirstDay, RankingType.WEEKLY);
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }


     /*
    현재 진행중인 학과 랭킹 월간 조회
     */
     public DepartmentRankingResponse getCurrentMonthlyDepartmentRanking(Long userId){
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
        List<DepartmentRankingTemp> departmentRankingList = departmentRankingRepository.getFinishedDepartmentRanking(monthFirstDay, RankingType.MONTHLY);
        return buildDepartmentRankingResponse(departmentRankingList, userId);
    }

    private DepartmentRankingResponse buildDepartmentRankingResponse(List<DepartmentRankingTemp> departmentRankingList, Long userId) {
        DepartmentRankingEntryResponse myRanking = null;
        List<DepartmentRankingEntryResponse> topRankings = new ArrayList<>();
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ExceptionType.USER_NOT_FOUND));
        for (DepartmentRankingTemp temp : departmentRankingList) {
            DepartmentRankingEntryResponse entry = DepartmentRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(user.getDepartment() != null && user.getDepartment().getKoreanName().equals(temp.getDepartmentName())){
                myRanking = entry;
            }
        }
        return new DepartmentRankingResponse(topRankings, myRanking);
    }
}

