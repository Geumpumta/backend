package com.gpt.geumpumtabackend.badge.service;

import com.gpt.geumpumtabackend.badge.domain.Badge;
import com.gpt.geumpumtabackend.badge.domain.BadgeType;
import com.gpt.geumpumtabackend.badge.domain.UserBadge;
import com.gpt.geumpumtabackend.badge.dto.request.BadgeCreateRequest;
import com.gpt.geumpumtabackend.badge.dto.request.RepresentativeBadgeRequest;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeCreateResponse;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeStatusResponse;
import com.gpt.geumpumtabackend.badge.dto.response.NewBadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.RepresentativeBadgeResponse;
import com.gpt.geumpumtabackend.badge.repository.BadgeRepository;
import com.gpt.geumpumtabackend.badge.repository.UserBadgeRepository;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.rank.domain.RankType;
import com.gpt.geumpumtabackend.rank.domain.SeasonRankingSnapshot;
import com.gpt.geumpumtabackend.rank.repository.SeasonRankingSnapshotRepository;
import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository;
import com.gpt.geumpumtabackend.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final StudySessionRepository studySessionRepository;
    private final StatisticsRepository statisticsRepository;
    private final SeasonRankingSnapshotRepository seasonRankingSnapshotRepository;

    private static final long STREAK_MIN_MILLIS = 30L * 60L * 1000L;

    public List<MyBadgeStatusResponse> getMyBadges(Long userId) {
        validateUserExists(userId);

        Map<Long, LocalDateTime> awardedAtByBadgeId = userBadgeRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        UserBadge::getBadgeId,
                        UserBadge::getAwardedAt,
                        (existing, ignored) -> existing
                ));

        return badgeRepository.findAll().stream()
                .sorted(Comparator.comparing(Badge::getId))
                .map(badge -> MyBadgeStatusResponse.from(badge, awardedAtByBadgeId.get(badge.getId())))
                .toList();
    }

    public List<BadgeResponse> getAllBadges() {
        return badgeRepository.findAll().stream()
                .map(BadgeResponse::from)
                .toList();
    }

    @Transactional
    public BadgeCreateResponse createBadge(BadgeCreateRequest request) {
        if (badgeRepository.existsByCode(request.code())) {
            throw new BusinessException(ExceptionType.BADGE_CODE_ALREADY_EXISTS);
        }

        Badge badge = Badge.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .iconUrl(request.iconUrl())
                .badgeType(request.badgeType())
                .thresholdValue(request.thresholdValue())
                .rank(request.rank())
                .build();

        Badge saved = badgeRepository.save(badge);
        return BadgeCreateResponse.from(saved);
    }

    @Transactional
    public void deleteBadge(Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new BusinessException(ExceptionType.BADGE_NOT_FOUND));

        if (userBadgeRepository.existsByBadgeId(badgeId)) {
            throw new BusinessException(ExceptionType.BADGE_IN_USE);
        }

        badgeRepository.delete(badge);
    }

    @Transactional
    public RepresentativeBadgeResponse setRepresentativeBadge(RepresentativeBadgeRequest request, Long userId) {
        Badge badge = badgeRepository.findByCode(request.badgeCode());
        if (badge == null) {
            throw new BusinessException(ExceptionType.BADGE_NOT_FOUND);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        if(!userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId()))
            throw new BusinessException(ExceptionType.BADGE_NOT_OWNED);

        user.setRepresentativeBadge(badge.getId());
        return RepresentativeBadgeResponse.from(badge);
    }

    @Transactional
    public NewBadgeResponse grantWelcomeBadge(Long userId) {
        validateUserExists(userId);

        Badge badge = badgeRepository.findByBadgeType(BadgeType.WELCOME);
        if (badge == null) {
            throw new BusinessException(ExceptionType.BADGE_NOT_FOUND);
        }
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
            return NewBadgeResponse.from(badge);
        }
        LocalDateTime now = LocalDateTime.now();
        userBadgeRepository.save(new UserBadge(userId, badge.getId(), now, now));
        return NewBadgeResponse.from(badge);
    }

    @Transactional
    public List<MyBadgeResponse> getUnnotifiedBadges(Long userId) {
        validateUserExists(userId);

        List<MyBadgeResponse> responses = userBadgeRepository.findUnnotifiedBadgeResponses(userId);
        if (responses.isEmpty()) {
            return Collections.emptyList();
        }

        markBadgesNotified(userId);

        return responses;
    }

    private void markBadgesNotified(Long userId) {
        List<UserBadge> userBadges = userBadgeRepository.findByUserIdAndNotifiedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        for (UserBadge userBadge : userBadges) {
            userBadge.markNotified(now);
        }
        userBadgeRepository.saveAll(userBadges);
    }

    @Transactional
    public List<NewBadgeResponse> grantStudyAchievementBadges(Long userId) {
        validateUserExists(userId);

        LocalDateTime now = LocalDateTime.now();
        List<NewBadgeResponse> newlyGranted = new ArrayList<>();

        grantTotalHoursBadge(userId, now, newlyGranted);
        grantStreakBadge(userId, now, newlyGranted);

        return newlyGranted;
    }

    @Transactional
    public int grantSeasonRankingBadges(Long seasonId) {
        return grantSeasonRankTypeBadges(
                seasonId,
                RankType.OVERALL,
                BadgeType.SEASON_PERSONAL_RANK
        );
    }

    private int grantSeasonRankTypeBadges(Long seasonId, RankType rankType, BadgeType badgeType) {
        List<Badge> badges = badgeRepository.findAllByBadgeType(badgeType);
        if (badges.isEmpty()) {
            return 0;
        }

        Map<Integer, Badge> badgeByRank = badges.stream()
                .filter(badge -> badge.getRank() != null)
                .collect(Collectors.toMap(
                        badge -> badge.getRank().intValue(),
                        badge -> badge,
                        (existing, ignored) -> existing
                ));
        if (badgeByRank.isEmpty()) {
            return 0;
        }

        Set<Integer> targetRanks = badgeByRank.keySet();
        List<SeasonRankingSnapshot> snapshots =
                seasonRankingSnapshotRepository.findBySeasonIdAndRankTypeAndFinalRankIn(
                        seasonId,
                        rankType,
                        targetRanks
                );

        int grantedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (SeasonRankingSnapshot snapshot : snapshots) {
            Badge badge = badgeByRank.get(snapshot.getFinalRank());
            if (badge == null) {
                continue;
            }
            if (userBadgeRepository.existsByUserIdAndBadgeId(snapshot.getUserId(), badge.getId())) {
                continue;
            }
            userBadgeRepository.save(new UserBadge(snapshot.getUserId(), badge.getId(), now, null));
            grantedCount++;
        }

        return grantedCount;
    }

    private void grantTotalHoursBadge(Long userId, LocalDateTime now, List<NewBadgeResponse> newlyGranted) {
        Long totalMillis = studySessionRepository.sumTotalStudyMillisByUserId(userId);
        if (totalMillis == null) {
            totalMillis = 0L;
        }

        List<Badge> hourBadges = badgeRepository.findAllByBadgeType(BadgeType.TOTAL_HOURS);
        for (Badge badge : hourBadges) {
            Long thresholdHours = badge.getThresholdValue();
            if (thresholdHours == null) {
                continue;
            }
            long thresholdMillis = thresholdHours * 60L * 60L * 1000L;
            if (totalMillis >= thresholdMillis
                    && !userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                userBadgeRepository.save(new UserBadge(userId, badge.getId(), now, now));
                newlyGranted.add(NewBadgeResponse.from(badge));
            }
        }
    }

    private void grantStreakBadge(Long userId, LocalDateTime now, List<NewBadgeResponse> newlyGranted) {
        int consecutiveDays = statisticsRepository.countCurrentConsecutiveStudyDays(
                userId, LocalDate.now(), STREAK_MIN_MILLIS
        );
        List<Badge> streakBadges = badgeRepository.findAllByBadgeType(BadgeType.STREAK_DAYS);
        for (Badge badge : streakBadges) {
            Long thresholdDays = badge.getThresholdValue();
            if (thresholdDays == null) {
                continue;
            }
            if (consecutiveDays >= thresholdDays
                    && !userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                userBadgeRepository.save(new UserBadge(userId, badge.getId(), now, now));
                newlyGranted.add(NewBadgeResponse.from(badge));
            }
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ExceptionType.USER_NOT_FOUND);
        }
    }
}
