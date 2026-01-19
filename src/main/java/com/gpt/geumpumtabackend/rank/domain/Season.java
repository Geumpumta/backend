package com.gpt.geumpumtabackend.rank.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeasonType seasonType;

    @Column(nullable = false, name = "start_date")
    private LocalDate startDate;

    @Column(nullable = false, name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeasonStatus status;

    @Builder
    public Season(String name, SeasonType seasonType, LocalDate startDate,
                  LocalDate endDate, SeasonStatus status) {
        validateDates(startDate, endDate);
        this.name = name;
        this.seasonType = seasonType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public void end() {
        if (this.status != SeasonStatus.ACTIVE) {
            throw new BusinessException(ExceptionType.SEASON_ALREADY_ENDED);
        }
        this.status = SeasonStatus.ENDED;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (end == null || start == null) {
            throw new BusinessException(ExceptionType.SEASON_INVALID_DATE_RANGE);
        }
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new BusinessException(ExceptionType.SEASON_INVALID_DATE_RANGE);
        }
    }
}
