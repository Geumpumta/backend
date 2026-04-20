package com.gpt.geumpumtabackend.rankbenchmark.mysql;

import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.user.domain.Department;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class MysqlRankingReader {

    private final StudySessionRepository studySessionRepository;
    private final UserRepository userRepository;

    public PersonalRankingResponse personalDaily(Long userId) {
        LocalDate today = LocalDate.now();
        return personalRange(userId, today.atStartOfDay(), today.atTime(23, 59, 59));
    }

    public PersonalRankingResponse personalWeekly(Long userId) {
        LocalDate today = LocalDate.now();
        return personalRange(userId,
                today.with(DayOfWeek.MONDAY).atStartOfDay(),
                today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59));
    }

    public PersonalRankingResponse personalMonthly(Long userId) {
        LocalDate today = LocalDate.now();
        return personalRange(userId,
                today.withDayOfMonth(1).atStartOfDay(),
                today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59));
    }

    public DepartmentRankingResponse departmentDaily(Long userId) {
        LocalDate today = LocalDate.now();
        return departmentRange(userId, today.atStartOfDay(), today.atTime(23, 59, 59));
    }

    public DepartmentRankingResponse departmentWeekly(Long userId) {
        LocalDate today = LocalDate.now();
        return departmentRange(userId,
                today.with(DayOfWeek.MONDAY).atStartOfDay(),
                today.with(DayOfWeek.SUNDAY).atTime(23, 59, 59));
    }

    public DepartmentRankingResponse departmentMonthly(Long userId) {
        LocalDate today = LocalDate.now();
        return departmentRange(userId,
                today.withDayOfMonth(1).atStartOfDay(),
                today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59));
    }

    private PersonalRankingResponse personalRange(Long userId, LocalDateTime start, LocalDateTime end) {
        List<PersonalRankingTemp> rows =
                studySessionRepository.calculateCurrentPeriodRanking(start, end, LocalDateTime.now());

        List<PersonalRankingEntryResponse> topRanks = new ArrayList<>();
        PersonalRankingEntryResponse my = null;
        for (PersonalRankingTemp row : rows) {
            PersonalRankingEntryResponse entry = PersonalRankingEntryResponse.of(row);
            topRanks.add(entry);
            if (userId != null && userId.equals(row.getUserId())) {
                my = entry;
            }
        }
        if (my == null && userId != null) {
            my = fallbackMy(userId, topRanks.size());
        }
        return new PersonalRankingResponse(topRanks, my);
    }

    private DepartmentRankingResponse departmentRange(Long userId, LocalDateTime start, LocalDateTime end) {
        List<DepartmentRankingTemp> rows =
                studySessionRepository.calculateCurrentDepartmentRanking(start, end, LocalDateTime.now());

        List<DepartmentRankingEntryResponse> topRanks = new ArrayList<>();
        DepartmentRankingEntryResponse my = null;
        String myDept = null;
        if (userId != null) {
            myDept = userRepository.findById(userId)
                    .map(u -> u.getDepartment() != null ? u.getDepartment().name() : null)
                    .orElse(null);
        }
        for (DepartmentRankingTemp row : rows) {
            DepartmentRankingEntryResponse entry = DepartmentRankingEntryResponse.of(row);
            if (row.getTotalMillis() != null && row.getTotalMillis() > 0) {
                topRanks.add(entry);
            }
            if (myDept != null && row.getDepartment() != null && myDept.equals(row.getDepartment())) {
                my = entry;
            }
        }
        return new DepartmentRankingResponse(topRanks, my);
    }

    private PersonalRankingEntryResponse fallbackMy(Long userId, int currentSize) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new PersonalRankingEntryResponse(userId, 0L, (long) currentSize + 1, null, null, null);
        }
        String departmentKorean = user.getDepartment() != null ? user.getDepartment().getKoreanName() : null;
        return new PersonalRankingEntryResponse(
                userId, 0L, (long) currentSize + 1,
                user.getName(), user.getPicture(), departmentKorean);
    }
}
