package com.gpt.geumpumtabackend.badge.repository;

import com.gpt.geumpumtabackend.badge.domain.UserBadge;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    @Query("""
        select new com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse(
            b.code, b.name, b.description, b.iconUrl, ub.awardedAt
        )
        from UserBadge ub
        join Badge b on b.id = ub.badgeId
        where ub.userId = :userId
        order by ub.awardedAt desc
    """)
    List<MyBadgeResponse> findMyBadges(Long userId);

    @Query("""
        select new com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse(
            b.code, b.name, b.description, b.iconUrl, ub.awardedAt
        )
        from UserBadge ub
        join Badge b on b.id = ub.badgeId
        where ub.userId = :userId
          and ub.notifiedAt is null
        order by ub.awardedAt desc
    """)
    List<MyBadgeResponse> findUnnotifiedBadgeResponses(Long userId);

    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    boolean existsByBadgeId(Long badgeId);

    List<UserBadge> findByUserId(Long userId);

    List<UserBadge> findByUserIdAndNotifiedAtIsNull(Long userId);

}
