package com.gpt.geumpumtabackend.global.scheduler;

import com.gpt.geumpumtabackend.token.repository.RefreshTokenRepository;
import com.gpt.geumpumtabackend.token.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class RefreshTokenDeleteScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredToken() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteAllRefreshToken(now);
        userSessionRepository.deleteExpiredSessions(now);
    }
}
