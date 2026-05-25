package com.gpt.geumpumtabackend.token.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
public class UserSession extends BaseEntity {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserSessionStatus status;

    @Column(unique = true)
    private String refreshToken;

    @Column(length = 255)
    private String fcmToken;

    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public UserSession(String sessionId, Long userId, String refreshToken, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = UserSessionStatus.ACTIVE;
        this.refreshToken = refreshToken;
        this.lastSeenAt = LocalDateTime.now(KST);
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.status = UserSessionStatus.REVOKED;
        this.refreshToken = null;
        this.fcmToken = null;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime expiresAt) {
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void clearFcmToken() {
        this.fcmToken = null;
    }

    public void touchLastSeen() {
        this.lastSeenAt = LocalDateTime.now(KST);
    }

    public boolean isActive() {
        return this.status == UserSessionStatus.ACTIVE;
    }

    public boolean isExpired(LocalDateTime now) {
        return !this.expiresAt.isAfter(now);
    }
}
