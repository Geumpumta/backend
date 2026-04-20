package com.gpt.geumpumtabackend.unit.rank.service;

import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.SeasonStatus;
import com.gpt.geumpumtabackend.rank.domain.SeasonType;
import com.gpt.geumpumtabackend.rank.repository.SeasonRepository;
import com.gpt.geumpumtabackend.rank.service.SeasonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("SeasonService 단위 테스트")
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @InjectMocks
    private SeasonService seasonService;

    @Test
    @DisplayName("날짜 기반으로 활성 시즌을 조회할 수 있다")
    void testGetActiveSeason() {
        // given
        LocalDate today = LocalDate.of(2024, 5, 15);
        Season mockSeason = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ACTIVE)
            .build();

        when(seasonRepository.findByDateRange(any(LocalDate.class)))
            .thenReturn(Optional.of(mockSeason));

        // when
        Season activeSeason = seasonService.getActiveSeason();

        // then
        assertThat(activeSeason).isNotNull();
        assertThat(activeSeason.getName()).isEqualTo("2024 1학기 시즌");
        assertThat(activeSeason.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
        verify(seasonRepository, times(1)).findByDateRange(any(LocalDate.class));
    }

    @Test
    @DisplayName("활성 시즌이 없으면 예외가 발생한다")
    void testGetActiveSeasonNotFound() {
        // given
        when(seasonRepository.findByDateRange(any(LocalDate.class)))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> seasonService.getActiveSeason())
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.NO_ACTIVE_SEASON);
    }

    @Test
    @DisplayName("초기 시즌을 생성할 수 있다 - 1학기")
    void testCreateInitialSeasonSpringSemester() {
        // given
        Season savedSeason = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ACTIVE)
            .build();

        when(seasonRepository.save(any(Season.class))).thenReturn(savedSeason);

        // when
        Season initialSeason = seasonService.createInitialSeason();

        // then
        assertThat(initialSeason).isNotNull();
        assertThat(initialSeason.getStatus()).isEqualTo(SeasonStatus.ACTIVE);
        verify(seasonRepository, times(1)).save(any(Season.class));
    }

    @Test
    @DisplayName("시즌 전환이 올바르게 수행된다 - 1학기에서 여름방학으로")
    void testTransitionToNextSeason_SpringToSummer() {
        // given
        Season currentSeason = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ACTIVE)
            .build();

        ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);

        // when
        seasonService.transitionToNextSeason(currentSeason);

        // then
        verify(seasonRepository, times(2)).save(seasonCaptor.capture());

        Season nextSeason = seasonCaptor.getAllValues().get(0);
        Season endedSeason = seasonCaptor.getAllValues().get(1);

        // 다음 시즌 검증
        assertThat(nextSeason.getSeasonType()).isEqualTo(SeasonType.SUMMER_VACATION);
        assertThat(nextSeason.getStartDate()).isEqualTo(LocalDate.of(2024, 7, 1));
        assertThat(nextSeason.getEndDate()).isEqualTo(LocalDate.of(2024, 8, 31));
        assertThat(nextSeason.getStatus()).isEqualTo(SeasonStatus.ACTIVE);

        // 이전 시즌 검증
        assertThat(endedSeason.getStatus()).isEqualTo(SeasonStatus.ENDED);
    }

    @Test
    @DisplayName("시즌 전환이 올바르게 수행된다 - 여름방학에서 2학기로")
    void testTransitionToNextSeason_SummerToFall() {
        // given
        Season currentSeason = Season.builder()
            .name("2024 여름방학 시즌")
            .seasonType(SeasonType.SUMMER_VACATION)
            .startDate(LocalDate.of(2024, 7, 1))
            .endDate(LocalDate.of(2024, 8, 31))
            .status(SeasonStatus.ACTIVE)
            .build();

        ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);

        // when
        seasonService.transitionToNextSeason(currentSeason);

        // then
        verify(seasonRepository, times(2)).save(seasonCaptor.capture());
        Season nextSeason = seasonCaptor.getAllValues().get(0);

        assertThat(nextSeason.getSeasonType()).isEqualTo(SeasonType.FALL_SEMESTER);
        assertThat(nextSeason.getStartDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(nextSeason.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @DisplayName("시즌 전환이 올바르게 수행된다 - 2학기에서 겨울방학으로")
    void testTransitionToNextSeason_FallToWinter() {
        // given
        Season currentSeason = Season.builder()
            .name("2024 2학기 시즌")
            .seasonType(SeasonType.FALL_SEMESTER)
            .startDate(LocalDate.of(2024, 9, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .status(SeasonStatus.ACTIVE)
            .build();

        ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);

        // when
        seasonService.transitionToNextSeason(currentSeason);

        // then
        verify(seasonRepository, times(2)).save(seasonCaptor.capture());
        Season nextSeason = seasonCaptor.getAllValues().get(0);

        assertThat(nextSeason.getSeasonType()).isEqualTo(SeasonType.WINTER_VACATION);
        assertThat(nextSeason.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        // 2025년은 윤년이 아니므로 2월 28일
        assertThat(nextSeason.getEndDate()).isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @Test
    @DisplayName("시즌 전환이 올바르게 수행된다 - 겨울방학에서 1학기로 (윤년)")
    void testTransitionToNextSeason_WinterToSpring_LeapYear() {
        // given - 2024년은 윤년
        Season currentSeason = Season.builder()
            .name("2024 겨울방학 시즌")
            .seasonType(SeasonType.WINTER_VACATION)
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 2, 29))  // 윤년
            .status(SeasonStatus.ACTIVE)
            .build();

        ArgumentCaptor<Season> seasonCaptor = ArgumentCaptor.forClass(Season.class);

        // when
        seasonService.transitionToNextSeason(currentSeason);

        // then
        verify(seasonRepository, times(2)).save(seasonCaptor.capture());
        Season nextSeason = seasonCaptor.getAllValues().get(0);

        assertThat(nextSeason.getSeasonType()).isEqualTo(SeasonType.SPRING_SEMESTER);
        assertThat(nextSeason.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(nextSeason.getEndDate()).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    @Test
    @DisplayName("이미 종료된 시즌은 다시 종료할 수 없다")
    void testCannotEndAlreadyEndedSeason() {
        // given
        Season endedSeason = Season.builder()
            .name("2024 1학기 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 6, 30))
            .status(SeasonStatus.ENDED)
            .build();

        // when & then
        assertThatThrownBy(() -> endedSeason.end())
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.SEASON_ALREADY_ENDED);
    }

    @Test
    @DisplayName("시즌 날짜 검증 - 종료일이 시작일보다 이전이면 예외가 발생한다")
    void testInvalidSeasonDateRange() {
        // given & when & then
        assertThatThrownBy(() -> Season.builder()
            .name("잘못된 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 6, 30))
            .endDate(LocalDate.of(2024, 3, 1))  // 시작일보다 이전
            .status(SeasonStatus.ACTIVE)
            .build())
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.SEASON_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("시즌 날짜 검증 - 종료일과 시작일이 같으면 예외가 발생한다")
    void testSameStartAndEndDate() {
        // given & when & then
        assertThatThrownBy(() -> Season.builder()
            .name("잘못된 시즌")
            .seasonType(SeasonType.SPRING_SEMESTER)
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 3, 1))  // 시작일과 동일
            .status(SeasonStatus.ACTIVE)
            .build())
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.SEASON_INVALID_DATE_RANGE);
    }
}
