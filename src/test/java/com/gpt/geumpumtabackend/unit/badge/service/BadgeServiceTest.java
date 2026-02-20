package com.gpt.geumpumtabackend.unit.badge.service;

import com.gpt.geumpumtabackend.badge.domain.Badge;
import com.gpt.geumpumtabackend.badge.domain.BadgeType;
import com.gpt.geumpumtabackend.badge.domain.UserBadge;
import com.gpt.geumpumtabackend.badge.dto.request.BadgeCreateRequest;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeCreateResponse;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeStatusResponse;
import com.gpt.geumpumtabackend.badge.dto.response.NewBadgeResponse;
import com.gpt.geumpumtabackend.badge.repository.BadgeRepository;
import com.gpt.geumpumtabackend.badge.repository.UserBadgeRepository;
import com.gpt.geumpumtabackend.badge.service.BadgeService;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.RankType;
import com.gpt.geumpumtabackend.rank.domain.SeasonRankingSnapshot;
import com.gpt.geumpumtabackend.rank.repository.SeasonRankingSnapshotRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.statistics.repository.StatisticsRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeService 단위 테스트")
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private StatisticsRepository statisticsRepository;

    @Mock
    private SeasonRankingSnapshotRepository seasonRankingSnapshotRepository;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    @DisplayName("관리자가 배지를 생성하면 저장 후 응답을 반환한다")
    void 배지를_생성하면_응답을_반환한다() {
        // Given
        BadgeCreateRequest request = new BadgeCreateRequest(
                "WELCOME_001",
                "웰컴 배지",
                "회원가입 축하 배지",
                "https://example.com/welcome.png",
                BadgeType.WELCOME,
                0L,
                null
        );
        Badge savedBadge = createBadge("WELCOME_001", BadgeType.WELCOME, null);
        ReflectionTestUtils.setField(savedBadge, "id", 100L);

        when(badgeRepository.existsByCode(request.code())).thenReturn(false);
        when(badgeRepository.save(any(Badge.class))).thenReturn(savedBadge);

        // When
        BadgeCreateResponse response = badgeService.createBadge(request);

        // Then
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.code()).isEqualTo("WELCOME_001");
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    @DisplayName("중복 코드로 배지 생성 시 BADGE_CODE_ALREADY_EXISTS 예외가 발생한다")
    void 중복코드_배지생성시_예외발생() {
        // Given
        BadgeCreateRequest request = new BadgeCreateRequest(
                "WELCOME_001",
                "웰컴 배지",
                "회원가입 축하 배지",
                "https://example.com/welcome.png",
                BadgeType.WELCOME,
                0L,
                null
        );
        when(badgeRepository.existsByCode(request.code())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> badgeService.createBadge(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.BADGE_CODE_ALREADY_EXISTS);
        verify(badgeRepository, never()).save(any(Badge.class));
    }

    @Test
    @DisplayName("관리자가 전체 배지를 조회하면 배지 목록을 반환한다")
    void 전체_배지를_조회하면_목록을_반환한다() {
        // Given
        Badge badge1 = createBadge("WELCOME_001", BadgeType.WELCOME, null);
        Badge badge2 = createBadge("TOTAL_HOURS_50", BadgeType.TOTAL_HOURS, null);
        when(badgeRepository.findAll()).thenReturn(List.of(badge1, badge2));

        // When
        List<BadgeResponse> responses = badgeService.getAllBadges();

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(BadgeResponse::code)
                .containsExactly("WELCOME_001", "TOTAL_HOURS_50");
    }

    @Test
    @DisplayName("내 배지 조회 시 전체 배지를 반환하고 보유 여부를 표시한다")
    void 내배지_조회시_전체배지와_보유여부를_반환한다() {
        // Given
        Long userId = 1L;
        Badge badge1 = createBadge("WELCOME_001", BadgeType.WELCOME, null);
        Badge badge2 = createBadge("TOTAL_HOURS_50", BadgeType.TOTAL_HOURS, null);
        ReflectionTestUtils.setField(badge1, "id", 1L);
        ReflectionTestUtils.setField(badge2, "id", 2L);
        LocalDateTime awardedAt = LocalDateTime.of(2026, 2, 1, 10, 0);
        UserBadge ownedBadge = new UserBadge(userId, 1L, awardedAt, awardedAt);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(badgeRepository.findAll()).thenReturn(List.of(badge2, badge1));
        when(userBadgeRepository.findByUserId(userId)).thenReturn(List.of(ownedBadge));

        // When
        List<MyBadgeStatusResponse> responses = badgeService.getMyBadges(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(MyBadgeStatusResponse::code)
                .containsExactly("WELCOME_001", "TOTAL_HOURS_50");
        assertThat(responses.get(0).owned()).isTrue();
        assertThat(responses.get(0).awardedAt()).isEqualTo(awardedAt);
        assertThat(responses.get(1).owned()).isFalse();
        assertThat(responses.get(1).awardedAt()).isNull();
    }

    @Test
    @DisplayName("지급된 이력이 없는 배지는 삭제한다")
    void 지급이력_없는_배지는_삭제한다() {
        // Given
        Long badgeId = 30L;
        Badge badge = createBadge("TOTAL_HOURS_100", BadgeType.TOTAL_HOURS, null);
        ReflectionTestUtils.setField(badge, "id", badgeId);
        when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));
        when(userBadgeRepository.existsByBadgeId(badgeId)).thenReturn(false);

        // When
        badgeService.deleteBadge(badgeId);

        // Then
        verify(badgeRepository, times(1)).delete(badge);
    }

    @Test
    @DisplayName("이미 지급된 배지는 삭제 시 BADGE_IN_USE 예외가 발생한다")
    void 이미_지급된_배지는_삭제할수없다() {
        // Given
        Long badgeId = 31L;
        Badge badge = createBadge("TOTAL_HOURS_200", BadgeType.TOTAL_HOURS, null);
        ReflectionTestUtils.setField(badge, "id", badgeId);
        when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));
        when(userBadgeRepository.existsByBadgeId(badgeId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> badgeService.deleteBadge(badgeId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("exceptionType", ExceptionType.BADGE_IN_USE);
        verify(badgeRepository, never()).delete(any(Badge.class));
    }

    @Test
    @DisplayName("회원가입 후 웰컴 배지가 정상 반환된다")
    void 회원가입후_웰컴배지가_반환된다() {
        // Given
        Long userId = 1L;
        Badge badge = createBadge("WELCOME_001", BadgeType.WELCOME, 10L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(badgeRepository.findByBadgeType(BadgeType.WELCOME)).thenReturn(badge);
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())).thenReturn(false);
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        NewBadgeResponse granted = badgeService.grantWelcomeBadge(userId);

        // Then
        assertThat(granted.code()).isEqualTo(badge.getCode());

        // mock 메서드 호출 시 전달된 인자를 캡처해서, 전달된 데이터 검증
        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository, times(1)).save(captor.capture());
        UserBadge saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getBadgeId()).isEqualTo(badge.getId());
        assertThat(saved.getNotifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("미확인 배지 조회 시 배지를 반환하고 확인 처리한다")
    void 미확인_배지_조회시_배지를_반환하고_확인처리한다() {
        // Given
        Long userId = 2L;
        Badge badge1 = createBadge("SEASON_2025_RANK_1", BadgeType.SEASON_PERSONAL_RANK, 1L);
        Badge badge2 = createBadge("SEASON_2025_RANK_2", BadgeType.SEASON_PERSONAL_RANK, 2L);
        LocalDateTime awardedAt1 = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime awardedAt2 = LocalDateTime.of(2026, 1, 2, 11, 0);
        UserBadge userBadge1 = new UserBadge(userId, badge1.getId(), awardedAt1, null);
        UserBadge userBadge2 = new UserBadge(userId, badge2.getId(), awardedAt2, null);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userBadgeRepository.findUnnotifiedBadgeResponses(userId)).thenReturn(List.of(
                new MyBadgeResponse(badge1.getCode(), badge1.getName(), badge1.getDescription(), badge1.getIconUrl(), awardedAt1),
                new MyBadgeResponse(badge2.getCode(), badge2.getName(), badge2.getDescription(), badge2.getIconUrl(), awardedAt2)
        ));
        when(userBadgeRepository.findByUserIdAndNotifiedAtIsNull(userId)).thenReturn(List.of(userBadge1, userBadge2));
        when(userBadgeRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<MyBadgeResponse> responses = badgeService.getUnnotifiedBadges(userId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(MyBadgeResponse::code)
                .containsExactlyInAnyOrder(badge1.getCode(), badge2.getCode());
        assertThat(responses)
                .extracting(MyBadgeResponse::awardedAt)
                .containsExactlyInAnyOrder(awardedAt1, awardedAt2);

        // mock 메서드 호출 시 전달된 인자를 캡처해서, 전달된 데이터 검증
        ArgumentCaptor<List<UserBadge>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userBadgeRepository, times(1)).saveAll(listCaptor.capture());
        List<UserBadge> saved = listCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(ub -> assertThat(ub.getNotifiedAt()).isNotNull());
        LocalDateTime notifiedAt = saved.get(0).getNotifiedAt();
        assertThat(saved)
                .extracting(UserBadge::getNotifiedAt)
                .allMatch(time -> time.equals(notifiedAt));
    }

    @Test
    @DisplayName("대표 배지 설정 시 배지 코드로 조회해 대표 배지를 설정한다")
    void 대표_배지_설정시_코드로_배지를_찾아_설정한다() {
        // Given
        Long userId = 10L;
        Badge badge = createBadge("WELCOME_001", BadgeType.WELCOME, 10L);
        User user = mock(User.class);

        when(badgeRepository.findByCode("WELCOME_001")).thenReturn(badge);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())).thenReturn(true);

        // When
        badgeService.setRepresentativeBadge(new com.gpt.geumpumtabackend.badge.dto.request.RepresentativeBadgeRequest("WELCOME_001"), userId);

        // Then
        verify(user, times(1)).setRepresentativeBadge(badge.getId());
    }

    @Test
    @DisplayName("누적 공부시간 기준 배지를 미보유하면 발급한다")
    void 누적_공부시간_기준_배지를_미보유시_발급한다() {
        // Given
        Long userId = 3L;
        Badge badge = createBadge("TOTAL_HOURS_50", BadgeType.TOTAL_HOURS, 1L);
        ReflectionTestUtils.setField(badge, "thresholdValue", 50L);
        long totalMillis = 50L * 60L * 60L * 1000L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(studySessionRepository.sumTotalStudyMillisByUserId(userId)).thenReturn(totalMillis);
        when(badgeRepository.findAllByBadgeType(BadgeType.TOTAL_HOURS)).thenReturn(List.of(badge));
        when(statisticsRepository.countCurrentConsecutiveStudyDays(eq(userId), any(), anyLong())).thenReturn(0);
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())).thenReturn(false);
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<NewBadgeResponse> responses = badgeService.grantStudyAchievementBadges(userId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).code()).isEqualTo(badge.getCode());
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("누적 공부시간 기준 배지를 이미 보유하면 발급하지 않는다")
    void 누적_공부시간_기준_배지를_이미_보유한_경우_발급하지_않는다() {
        // Given
        Long userId = 4L;
        Badge badge = createBadge("TOTAL_HOURS_50", BadgeType.TOTAL_HOURS, 1L);
        ReflectionTestUtils.setField(badge, "thresholdValue", 50L);
        long totalMillis = 60L * 60L * 60L * 1000L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(studySessionRepository.sumTotalStudyMillisByUserId(userId)).thenReturn(totalMillis);
        when(badgeRepository.findAllByBadgeType(BadgeType.TOTAL_HOURS)).thenReturn(List.of(badge));
        when(statisticsRepository.countCurrentConsecutiveStudyDays(eq(userId), any(), anyLong())).thenReturn(0);
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())).thenReturn(true);

        // When
        List<NewBadgeResponse> responses = badgeService.grantStudyAchievementBadges(userId);

        // Then
        assertThat(responses).isEmpty();
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("누적 공부시간 기준 미달이면 배지를 발급하지 않는다")
    void 누적_공부시간_기준_미달이면_배지를_발급하지_않는다() {
        // Given
        Long userId = 5L;
        Badge badge = createBadge("TOTAL_HOURS_50", BadgeType.TOTAL_HOURS, 1L);
        ReflectionTestUtils.setField(badge, "thresholdValue", 50L);
        long totalMillis = 49L * 60L * 60L * 1000L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(studySessionRepository.sumTotalStudyMillisByUserId(userId)).thenReturn(totalMillis);
        when(badgeRepository.findAllByBadgeType(BadgeType.TOTAL_HOURS)).thenReturn(List.of(badge));
        when(statisticsRepository.countCurrentConsecutiveStudyDays(eq(userId), any(), anyLong())).thenReturn(0);

        // When
        List<NewBadgeResponse> responses = badgeService.grantStudyAchievementBadges(userId);

        // Then
        assertThat(responses).isEmpty();
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("연속 공부일수 기준 배지를 미보유하면 발급한다")
    void 연속_공부일수_기준_배지를_미보유시_발급한다() {
        // Given
        Long userId = 6L;
        Badge badge = createBadge("STREAK_DAYS_7", BadgeType.STREAK_DAYS, 1L);
        ReflectionTestUtils.setField(badge, "thresholdValue", 7L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(studySessionRepository.sumTotalStudyMillisByUserId(userId)).thenReturn(0L);
        when(badgeRepository.findAllByBadgeType(BadgeType.TOTAL_HOURS)).thenReturn(List.of());
        when(statisticsRepository.countCurrentConsecutiveStudyDays(eq(userId), any(), anyLong())).thenReturn(7);
        when(badgeRepository.findAllByBadgeType(BadgeType.STREAK_DAYS)).thenReturn(List.of(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())).thenReturn(false);
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<NewBadgeResponse> responses = badgeService.grantStudyAchievementBadges(userId);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).code()).isEqualTo(badge.getCode());
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("연속 공부일수 기준 미달이면 배지를 발급하지 않는다")
    void 연속_공부일수_기준_미달이면_배지를_발급하지_않는다() {
        // Given
        Long userId = 7L;
        Badge badge = createBadge("STREAK_DAYS_7", BadgeType.STREAK_DAYS, 1L);
        ReflectionTestUtils.setField(badge, "thresholdValue", 7L);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(studySessionRepository.sumTotalStudyMillisByUserId(userId)).thenReturn(0L);
        when(badgeRepository.findAllByBadgeType(BadgeType.TOTAL_HOURS)).thenReturn(List.of());
        when(statisticsRepository.countCurrentConsecutiveStudyDays(eq(userId), any(), anyLong())).thenReturn(6);
        when(badgeRepository.findAllByBadgeType(BadgeType.STREAK_DAYS)).thenReturn(List.of(badge));

        // When
        List<NewBadgeResponse> responses = badgeService.grantStudyAchievementBadges(userId);

        // Then
        assertThat(responses).isEmpty();
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("시즌 전체랭킹 배지를 지급한다")
    void 시즌_전체랭킹_배지를_지급한다() {
        // Given
        Long seasonId = 100L;
        Long userId = 11L;
        Badge overallRank1Badge = createBadge("SEASON_OVERALL_1", BadgeType.SEASON_PERSONAL_RANK, 1L);
        SeasonRankingSnapshot snapshot = SeasonRankingSnapshot.builder()
                .seasonId(seasonId)
                .userId(userId)
                .rankType(RankType.OVERALL)
                .finalRank(1)
                .finalTotalMillis(1_000_000L)
                .snapshotAt(LocalDateTime.now())
                .build();

        when(badgeRepository.findAllByBadgeType(BadgeType.SEASON_PERSONAL_RANK))
                .thenReturn(List.of(overallRank1Badge));
        when(seasonRankingSnapshotRepository.findBySeasonIdAndRankTypeAndFinalRankIn(
                seasonId, RankType.OVERALL, Set.of(1)))
                .thenReturn(List.of(snapshot));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, overallRank1Badge.getId()))
                .thenReturn(false);
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int grantedCount = badgeService.grantSeasonRankingBadges(seasonId);

        // Then
        assertThat(grantedCount).isEqualTo(1);
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("시즌 랭킹 배지를 이미 보유하면 지급하지 않는다")
    void 시즌_랭킹_배지를_이미_보유하면_지급하지_않는다() {
        // Given
        Long seasonId = 101L;
        Long userId = 12L;
        Badge overallRank1Badge = createBadge("SEASON_OVERALL_1", BadgeType.SEASON_PERSONAL_RANK, 1L);
        SeasonRankingSnapshot snapshot = SeasonRankingSnapshot.builder()
                .seasonId(seasonId)
                .userId(userId)
                .rankType(RankType.OVERALL)
                .finalRank(1)
                .finalTotalMillis(1_000_000L)
                .snapshotAt(LocalDateTime.now())
                .build();

        when(badgeRepository.findAllByBadgeType(BadgeType.SEASON_PERSONAL_RANK))
                .thenReturn(List.of(overallRank1Badge));
        when(seasonRankingSnapshotRepository.findBySeasonIdAndRankTypeAndFinalRankIn(
                seasonId, RankType.OVERALL, Set.of(1)))
                .thenReturn(List.of(snapshot));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, overallRank1Badge.getId()))
                .thenReturn(true);

        // When
        int grantedCount = badgeService.grantSeasonRankingBadges(seasonId);

        // Then
        assertThat(grantedCount).isZero();
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("시즌 전체랭킹 1,2,3등 유저에게 각 등수 배지를 지급한다")
    void 시즌_전체랭킹_1_2_3등에_각각_해당_배지를_지급한다() {
        // Given
        Long seasonId = 102L;
        Long user1 = 21L;
        Long user2 = 22L;
        Long user3 = 23L;

        Badge rank1Badge = createBadge("SEASON_OVERALL_1", BadgeType.SEASON_PERSONAL_RANK, 1L);
        Badge rank2Badge = createBadge("SEASON_OVERALL_2", BadgeType.SEASON_PERSONAL_RANK, 2L);
        Badge rank3Badge = createBadge("SEASON_OVERALL_3", BadgeType.SEASON_PERSONAL_RANK, 3L);

        SeasonRankingSnapshot snapshot1 = SeasonRankingSnapshot.builder()
                .seasonId(seasonId)
                .userId(user1)
                .rankType(RankType.OVERALL)
                .finalRank(1)
                .finalTotalMillis(3_000_000L)
                .snapshotAt(LocalDateTime.now())
                .build();
        SeasonRankingSnapshot snapshot2 = SeasonRankingSnapshot.builder()
                .seasonId(seasonId)
                .userId(user2)
                .rankType(RankType.OVERALL)
                .finalRank(2)
                .finalTotalMillis(2_000_000L)
                .snapshotAt(LocalDateTime.now())
                .build();
        SeasonRankingSnapshot snapshot3 = SeasonRankingSnapshot.builder()
                .seasonId(seasonId)
                .userId(user3)
                .rankType(RankType.OVERALL)
                .finalRank(3)
                .finalTotalMillis(1_000_000L)
                .snapshotAt(LocalDateTime.now())
                .build();

        when(badgeRepository.findAllByBadgeType(BadgeType.SEASON_PERSONAL_RANK))
                .thenReturn(List.of(rank1Badge, rank2Badge, rank3Badge));
        when(seasonRankingSnapshotRepository.findBySeasonIdAndRankTypeAndFinalRankIn(
                seasonId, RankType.OVERALL, Set.of(1, 2, 3)))
                .thenReturn(List.of(snapshot3, snapshot1, snapshot2));
        when(userBadgeRepository.existsByUserIdAndBadgeId(anyLong(), anyLong()))
                .thenReturn(false);
        when(userBadgeRepository.save(any(UserBadge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int grantedCount = badgeService.grantSeasonRankingBadges(seasonId);

        // Then
        assertThat(grantedCount).isEqualTo(3);

        ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
        verify(userBadgeRepository, times(3)).save(captor.capture());
        List<UserBadge> saved = captor.getAllValues();
        Map<Long, Long> badgeByUser = saved.stream()
                .collect(Collectors.toMap(UserBadge::getUserId, UserBadge::getBadgeId));

        assertThat(badgeByUser).containsEntry(user1, rank1Badge.getId());
        assertThat(badgeByUser).containsEntry(user2, rank2Badge.getId());
        assertThat(badgeByUser).containsEntry(user3, rank3Badge.getId());
    }

    private Badge createBadge(String code, BadgeType type, Long rank) {
        Badge badge = Badge.builder()
                .code(code)
                .name("badge-name")
                .description("badge-desc")
                .iconUrl("http://example.com/badge.png")
                .badgeType(type)
                .thresholdValue(10L)
                .rank(rank)
                .build();
        ReflectionTestUtils.setField(badge, "id", rank);
        return badge;
    }
}
