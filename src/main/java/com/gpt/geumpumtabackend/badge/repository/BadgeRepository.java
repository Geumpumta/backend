package com.gpt.geumpumtabackend.badge.repository;

import com.gpt.geumpumtabackend.badge.domain.Badge;
import com.gpt.geumpumtabackend.badge.domain.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    Badge findByBadgeType(BadgeType badgeType);

    List<Badge> findAllByBadgeType(BadgeType badgeType);

    Badge findByCode(String code);

    boolean existsByCode(String code);
}
