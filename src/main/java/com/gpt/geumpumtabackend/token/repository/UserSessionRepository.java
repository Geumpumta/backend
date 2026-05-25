package com.gpt.geumpumtabackend.token.repository;

import com.gpt.geumpumtabackend.token.domain.UserSession;
import com.gpt.geumpumtabackend.token.domain.UserSessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionId(String sessionId);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    Optional<UserSession> findByFcmToken(String fcmToken);

    Optional<UserSession> findByUserIdAndStatus(Long userId, UserSessionStatus status);

    List<UserSession> findAllByUserIdAndStatus(Long userId, UserSessionStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE UserSession s
            SET s.status = com.gpt.geumpumtabackend.token.domain.UserSessionStatus.REVOKED,
                s.refreshToken = null,
                s.fcmToken = null
            WHERE s.userId = :userId
              AND s.status = com.gpt.geumpumtabackend.token.domain.UserSessionStatus.ACTIVE
            """)
    void revokeActiveSessionsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE UserSession s
            SET s.status = com.gpt.geumpumtabackend.token.domain.UserSessionStatus.REVOKED,
                s.refreshToken = null,
                s.fcmToken = null
            WHERE s.userId = :userId
            """)
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt <= :timeToDelete")
    void deleteExpiredSessions(@Param("timeToDelete") LocalDateTime timeToDelete);
}
