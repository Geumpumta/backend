package com.gpt.geumpumtabackend.token.repository;

import com.gpt.geumpumtabackend.token.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(Long userId);
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiredAt <= :timeToDelete")
    void deleteAllRefreshToken(LocalDateTime timeToDelete);

    boolean existsByUserId(Long userId);
}
