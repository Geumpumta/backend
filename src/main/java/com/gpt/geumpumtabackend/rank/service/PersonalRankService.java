package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
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
public class PersonalRankService {

    private final UserRankingRepository userRankingRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;
    /*
    현재 진행 중인 세션의 일간 랭킹 조회
     */
    public PersonalRankingResponse getCurrentDaily(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<PersonalRankingTemp> userRankingTempList = studySessionRepository.calculateCurrentPeriodRanking(startToday, endToday, nowTime);
        return buildPersonalRankingResponse(userRankingTempList, userId);
    }

    /*
    완료된 일간 랭킹 조회
     */
    public PersonalRankingResponse getCompletedDaily(Long userId, LocalDateTime day) {
        List<PersonalRankingTemp> userRankingTempList = userRankingRepository.getFinishedPersonalRanking(day, RankingType.DAILY);
        return buildPersonalRankingResponse(userRankingTempList, userId);
    }

    /*
    현재 진행 중인 세션의 주간 랭킹 조회
     */
    public PersonalRankingResponse getCurrentWeekly(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<PersonalRankingTemp> userRankingTempList = studySessionRepository.calculateCurrentPeriodRanking(weekStart, weekEnd, nowTime);
        return buildPersonalRankingResponse(userRankingTempList, userId);
    }

    /*
    완료된 주간 랭킹 조회
     */

    public PersonalRankingResponse getCompletedWeekly(Long userId, LocalDateTime weekFirstDay) {
        List<PersonalRankingTemp> userRankingTempList = userRankingRepository.getFinishedPersonalRanking(weekFirstDay, RankingType.WEEKLY);
        return buildPersonalRankingResponse(userRankingTempList, userId);
    }

    /*
    현재 진행 중인 세션의 월간 랭킹 조회
     */
    public PersonalRankingResponse getCurrentMonthly(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<PersonalRankingTemp> userRankingTempList = studySessionRepository.calculateCurrentPeriodRanking(startMonth, endMonth, nowTime);
        return buildPersonalRankingResponse(userRankingTempList, userId);
    }
    /*
    완료된 월간 랭킹 조회
     */
    public PersonalRankingResponse getCompletedMonthly(Long userId, LocalDateTime monthFirstDay) {
        List<PersonalRankingTemp> userRankingTempList = userRankingRepository.getFinishedPersonalRanking(monthFirstDay, RankingType.MONTHLY);
        return buildPersonalRankingResponse(userRankingTempList, userId);
    }

    private PersonalRankingResponse buildPersonalRankingResponse(List<PersonalRankingTemp> personalRankingList, Long userId) {
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        PersonalRankingEntryResponse myRanking = null;
        
        for (PersonalRankingTemp temp : personalRankingList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        
        // LEFT JOIN으로 모든 사용자가 포함되므로 이론적으로는 항상 찾아야 하지만, 만약을 위한 fallback
        if (myRanking == null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));
            String departmentName = user.getDepartment() != null ? user.getDepartment().getKoreanName() : null;
            myRanking = new PersonalRankingEntryResponse(
                userId, 
                0L, 
                (long) personalRankingList.size() + 1, 
                user.getName(),
                user.getPicture(),
                departmentName
            );
        }
        
        return new PersonalRankingResponse(topRankings, myRanking);
    }
}
