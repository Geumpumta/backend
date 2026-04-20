package com.gpt.geumpumtabackend.global.oauth.service;

import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtUserClaim;
import com.gpt.geumpumtabackend.global.exception.BusinessException;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.token.domain.RefreshToken;
import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.token.repository.RefreshTokenRepository;
import com.gpt.geumpumtabackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthLoginPolicyService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtHandler jwtHandler;

    @Transactional
    public Optional<Token> issueTokenIfNoActiveSession(JwtUserClaim jwtUserClaim) {
        userRepository.findByIdForUpdate(jwtUserClaim.userId())
                .orElseThrow(() -> new BusinessException(ExceptionType.USER_NOT_FOUND));

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(jwtUserClaim.userId());
        if (existingToken.isPresent()) {
            LocalDateTime now = LocalDateTime.now(KST);
            if (existingToken.get().getExpiredAt().isAfter(now)) {
                return Optional.empty();
            }
            refreshTokenRepository.deleteByUserId(jwtUserClaim.userId());
        }

        return Optional.of(jwtHandler.createTokens(jwtUserClaim));
    }
}
