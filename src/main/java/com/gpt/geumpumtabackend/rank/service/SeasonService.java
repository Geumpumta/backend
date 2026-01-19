package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.Season;
import com.gpt.geumpumtabackend.rank.domain.SeasonStatus;
import com.gpt.geumpumtabackend.rank.domain.SeasonType;
import com.gpt.geumpumtabackend.rank.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SeasonService {

    private final SeasonRepository seasonRepository;


    @Cacheable(value = "activeSeason", unless = "#result == null")
    public Season getActiveSeason() {
        LocalDate today = LocalDate.now();
        return seasonRepository.findByDateRange(today)
            .orElseThrow(() -> new BusinessException(ExceptionType.NO_ACTIVE_SEASON));
    }


    public Season getActiveSeasonNoCache() {
        LocalDate today = LocalDate.now();
        return seasonRepository.findByDateRange(today)
            .orElseThrow(() -> new BusinessException(ExceptionType.NO_ACTIVE_SEASON));
    }


    @Transactional
    public Season createInitialSeason() {
        LocalDate today = LocalDate.now();
        SeasonType seasonType = determineSeasonType(today);

        Season season = Season.builder()
            .name(generateSeasonName(today.getYear(), seasonType))
            .seasonType(seasonType)
            .startDate(getSeasonStartDate(today, seasonType))
            .endDate(getSeasonEndDate(today, seasonType))
            .status(SeasonStatus.ACTIVE)
            .build();

        Season savedSeason = seasonRepository.save(season);
        log.info("[SEASON] Initial season created: {}", savedSeason.getName());
        return savedSeason;
    }


    @Transactional
    public void transitionToNextSeason(Season currentSeason) {

        LocalDate nextStart = currentSeason.getEndDate().plusDays(1);
        SeasonType nextType = getNextSeasonType(currentSeason.getSeasonType());
        int year = nextStart.getYear();

        Season nextSeason = Season.builder()
            .name(generateSeasonName(year, nextType))
            .seasonType(nextType)
            .startDate(nextStart)
            .endDate(getSeasonEndDate(nextStart, nextType))
            .status(SeasonStatus.ACTIVE)
            .build();

        seasonRepository.save(nextSeason);

        currentSeason.end();
        seasonRepository.save(currentSeason);

        log.info("[SEASON] Transition completed: {} → {}",
                 currentSeason.getName(), nextSeason.getName());
    }


    private SeasonType determineSeasonType(LocalDate date) {
        int month = date.getMonthValue();
        if (month >= 3 && month <= 6) return SeasonType.SPRING_SEMESTER;
        if (month >= 7 && month <= 8) return SeasonType.SUMMER_VACATION;
        if (month >= 9 && month <= 12) return SeasonType.FALL_SEMESTER;
        return SeasonType.WINTER_VACATION;
    }


    private SeasonType getNextSeasonType(SeasonType current) {
        return switch (current) {
            case SPRING_SEMESTER -> SeasonType.SUMMER_VACATION;
            case SUMMER_VACATION -> SeasonType.FALL_SEMESTER;
            case FALL_SEMESTER -> SeasonType.WINTER_VACATION;
            case WINTER_VACATION -> SeasonType.SPRING_SEMESTER;
        };
    }


    private LocalDate getSeasonStartDate(LocalDate referenceDate, SeasonType type) {
        int year = referenceDate.getYear();
        return switch (type) {
            case SPRING_SEMESTER -> LocalDate.of(year, 3, 1);
            case SUMMER_VACATION -> LocalDate.of(year, 7, 1);
            case FALL_SEMESTER -> LocalDate.of(year, 9, 1);
            case WINTER_VACATION -> LocalDate.of(year, 1, 1);
        };
    }


    private LocalDate getSeasonEndDate(LocalDate startDate, SeasonType type) {
        int year = startDate.getYear();
        return switch (type) {
            case SPRING_SEMESTER -> LocalDate.of(year, 6, 30);
            case SUMMER_VACATION -> LocalDate.of(year, 8, 31);
            case FALL_SEMESTER -> LocalDate.of(year, 12, 31);
            case WINTER_VACATION -> {
                boolean isLeap = Year.isLeap(year);
                yield LocalDate.of(year, 2, isLeap ? 29 : 28);
            }
        };
    }


    private String generateSeasonName(int year, SeasonType type) {
        String typeName = switch (type) {
            case SPRING_SEMESTER -> "1학기";
            case SUMMER_VACATION -> "여름방학";
            case FALL_SEMESTER -> "2학기";
            case WINTER_VACATION -> "겨울방학";
        };
        return year + " " + typeName + " 시즌";
    }
}
