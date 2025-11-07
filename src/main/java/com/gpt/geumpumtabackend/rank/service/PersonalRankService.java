package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.rank.domain.RankingType;
import com.gpt.geumpumtabackend.rank.dto.UserRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
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
    /*
    현재 진행 중인 세션의 일간 랭킹 조회
     */
    public PersonalRankingResponse getCurrentDaily(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<UserRankingTemp> userRankingTempList = studySessionRepository.calculateCurrentPeriodRanking(startToday, endToday, nowTime);
        PersonalRankingEntryResponse myRanking = null;
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        for (UserRankingTemp temp : userRankingTempList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        return new PersonalRankingResponse(topRankings, myRanking);
    }

    /*
    완료된 일간 랭킹 조회
     */
    public PersonalRankingResponse getCompletedDaily(Long userId, LocalDateTime day) {
        List<com.gpt.geumpumtabackend.rank.dto.UserRankingTemp> userRankingTempList = userRankingRepository.getFinishedPersonalRanking(day, RankingType.DAILY);
        PersonalRankingEntryResponse myRanking = null;
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        for (UserRankingTemp temp : userRankingTempList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        return new PersonalRankingResponse(topRankings, myRanking);
    }

    /*
    현재 진행 중인 세션의 주간 랭킹 조회
     */
    public PersonalRankingResponse getCurrentWeekly(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<UserRankingTemp> userRankingTempList = studySessionRepository.calculateCurrentPeriodRanking(weekStart, weekEnd, nowTime);
        PersonalRankingEntryResponse myRanking = null;
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        for (UserRankingTemp temp : userRankingTempList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);
            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        return new PersonalRankingResponse(topRankings, myRanking);
    }

    /*
    완료된 주간 랭킹 조회
     */

    public PersonalRankingResponse getCompletedWeekly(Long userId, LocalDateTime weekFirstDay) {
        List<com.gpt.geumpumtabackend.rank.dto.UserRankingTemp> userRankingTempList = userRankingRepository.getFinishedPersonalRanking(weekFirstDay, RankingType.WEEKLY);
        PersonalRankingEntryResponse myRanking = null;
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        for (UserRankingTemp temp : userRankingTempList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        return new PersonalRankingResponse(topRankings, myRanking);
    }

    /*
    현재 진행 중인 세션의 월간 랭킹 조회
     */
    public PersonalRankingResponse getCurrentMonthly(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);
        LocalDateTime nowTime = LocalDateTime.now();
        List<UserRankingTemp> userRankingTempList = studySessionRepository.calculateCurrentPeriodRanking(startMonth, endMonth, nowTime);
        PersonalRankingEntryResponse myRanking = null;
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        for (UserRankingTemp temp : userRankingTempList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        return new PersonalRankingResponse(topRankings, myRanking);
    }
    /*
    완료된 월간 랭킹 조회
     */
    public PersonalRankingResponse getCompletedMonthly(Long userId, LocalDateTime monthFirstDay) {
        List<com.gpt.geumpumtabackend.rank.dto.UserRankingTemp> userRankingTempList = userRankingRepository.getFinishedPersonalRanking(monthFirstDay, RankingType.MONTHLY);
        PersonalRankingEntryResponse myRanking = null;
        List<PersonalRankingEntryResponse> topRankings = new ArrayList<>();
        for (UserRankingTemp temp : userRankingTempList) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(temp);
            topRankings.add(entry);

            if(temp.getUserId().equals(userId)){
                myRanking = entry;
            }
        }
        return new PersonalRankingResponse(topRankings, myRanking);
    }
}
