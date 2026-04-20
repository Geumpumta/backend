package com.gpt.geumpumtabackend.unit.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.Season;
import com.gpt.geumpumtabackend.rank.domain.SeasonStatus;
import com.gpt.geumpumtabackend.rank.domain.SeasonType;
import com.gpt.geumpumtabackend.rank.repository.SeasonRankingSnapshotRepository;
import com.gpt.geumpumtabackend.rank.repository.SeasonRepository;
import com.gpt.geumpumtabackend.rank.repository.UserRankingRepository;
import com.gpt.geumpumtabackend.rank.service.SeasonSnapshotBatchService;
import com.gpt.geumpumtabackend.rank.service.SeasonSnapshotService;
import com.gpt.geumpumtabackend.unit.config.BaseUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@DisplayName("SeasonSnapshotService 재시도 로직 테스트")
class SeasonSnapshotServiceRetryTest extends BaseUnitTest {

    @Autowired
    private SeasonSnapshotService seasonSnapshotService;

    @MockBean
    private SeasonRepository seasonRepository;

    @MockBean
    private SeasonRankingSnapshotRepository snapshotRepository;

    @MockBean
    private UserRankingRepository userRankingRepository;

    @MockBean
    private SeasonSnapshotBatchService batchService;

    @Test
    @DisplayName("스냅샷 생성 실패 시 최대 3회 재시도한다")
    void testRetryOnFailure() {
        // given
        Long seasonId = 1L;
        Season season = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ENDED)
            .build();

        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(snapshotRepository.existsBySeasonId(seasonId)).thenReturn(false);
        when(userRankingRepository.calculateSeasonRankingFromMonthlyRankings(any(), any()))
            .thenReturn(Collections.emptyList());

        // batchService에서 DataAccessException 발생
        when(batchService.saveBatchWithJdbc(any()))
            .thenThrow(new DataAccessException("DB connection failed") {});

        // when
        int result = seasonSnapshotService.createSeasonSnapshot(seasonId);

        // then
        // @Recover 메서드가 호출되어 0을 반환
        assertThat(result).isEqualTo(0);

        // 최대 3회 시도 확인 (원래 1회 + 재시도 2회)
        verify(batchService, times(3)).saveBatchWithJdbc(any());
    }

    @Test
    @DisplayName("첫 시도에서 성공하면 재시도하지 않는다")
    void testNoRetryOnSuccess() {
        // given
        Long seasonId = 1L;
        Season season = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ENDED)
            .build();

        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(snapshotRepository.existsBySeasonId(seasonId)).thenReturn(false);
        when(userRankingRepository.calculateSeasonRankingFromMonthlyRankings(any(), any()))
            .thenReturn(Collections.emptyList());
        when(batchService.saveBatchWithJdbc(any())).thenReturn(0);

        // when
        seasonSnapshotService.createSeasonSnapshot(seasonId);

        // then
        // 1회만 호출 (재시도 없음)
        verify(batchService, times(1)).saveBatchWithJdbc(any());
    }

    @Test
    @DisplayName("2번째 시도에서 성공하면 3번째 시도는 하지 않는다")
    void testRetrySuccessOnSecondAttempt() {
        // given
        Long seasonId = 1L;
        Season season = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ENDED)
            .build();

        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(snapshotRepository.existsBySeasonId(seasonId)).thenReturn(false);
        when(userRankingRepository.calculateSeasonRankingFromMonthlyRankings(any(), any()))
            .thenReturn(Collections.emptyList());

        // 첫 시도는 실패, 두 번째 시도는 성공
        when(batchService.saveBatchWithJdbc(any()))
            .thenThrow(new DataAccessException("First attempt failed") {})
            .thenReturn(0);

        // when
        seasonSnapshotService.createSeasonSnapshot(seasonId);

        // then
        // 2회만 호출 (첫 시도 + 재시도 1회)
        verify(batchService, times(2)).saveBatchWithJdbc(any());
    }

    @Test
    @DisplayName("이미 스냅샷이 존재하면 생성하지 않는다")
    void testSkipIfSnapshotExists() {
        // given
        Long seasonId = 1L;
        Season season = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ENDED)
            .build();

        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(snapshotRepository.existsBySeasonId(seasonId)).thenReturn(true);

        // when
        int result = seasonSnapshotService.createSeasonSnapshot(seasonId);

        // then
        assertThat(result).isEqualTo(0);
        verify(batchService, never()).saveBatchWithJdbc(any());
    }

    @Test
    @DisplayName("시즌이 존재하지 않으면 예외가 발생한다")
    void testThrowExceptionIfSeasonNotFound() {
        // given
        Long seasonId = 999L;
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.empty());

        // when & then
        try {
            seasonSnapshotService.createSeasonSnapshot(seasonId);
        } catch (BusinessException e) {
            assertThat(e.getExceptionType()).isEqualTo(ExceptionType.SEASON_NOT_FOUND);
        }

        verify(batchService, never()).saveBatchWithJdbc(any());
    }
}
