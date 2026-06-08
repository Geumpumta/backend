package com.gpt.geumpumtabackend.global.jwt;



import com.gpt.geumpumtabackend.token.domain.Token;
import com.gpt.geumpumtabackend.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class JwtHandler {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    public static final String USER_ID = "USER_ID";
    public static final String USER_ROLE = "ROLE_USER";
    public static final String SESSION_ID = "SESSION_ID";
    private static final String IS_WITHDRAWN = "WITHDRAWN";
    private static final long MILLI_SECOND = 1000L;

    public JwtHandler(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        secretKey = new SecretKeySpec(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public Token createTokens(JwtUserClaim jwtUserClaim, String refreshToken) {
        Map<String, Object> tokenClaims = this.createClaims(jwtUserClaim);
        Date now = new Date(System.currentTimeMillis());
        long accessTokenExpireIn = jwtProperties.getAccessTokenExpireIn();

        String accessToken = Jwts.builder()
                .claims(tokenClaims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpireIn * MILLI_SECOND))
                .signWith(secretKey)
                .compact();

        return Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Token createAccessToken(JwtUserClaim jwtUserClaim) {
        return createTokens(jwtUserClaim, null);
    }

    public Token createTokens(JwtUserClaim jwtUserClaim) {
        return createTokens(jwtUserClaim, null);
    }

    public Map<String, Object> createClaims(JwtUserClaim jwtUserClaim) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID, jwtUserClaim.userId());
        claims.put(USER_ROLE, jwtUserClaim.role());
        claims.put(IS_WITHDRAWN, jwtUserClaim.withdrawn());
        if (jwtUserClaim.sessionId() != null) {
            claims.put(SESSION_ID, jwtUserClaim.sessionId());
        }
        return claims;
    }


    // 재발급을 위해 token이 만료되었더라도 claim을 반환하는 메서드
    public Optional<JwtUserClaim> getClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(this.convert(claims));
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            return Optional.of(this.convert(claims));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public JwtUserClaim convert(Claims claims) {
        Boolean withdrawn = claims.get(IS_WITHDRAWN, Boolean.class);
        return new JwtUserClaim(
                claims.get(USER_ID, Long.class),
                claims.get(SESSION_ID, String.class),
                UserRole.valueOf(claims.get(USER_ROLE, String.class)),
                withdrawn != null ? withdrawn : false
        );
    }

    // 필터에서 토큰의 상태를 검증하기 위한 메서드 exception은 사용하는 곳에서 처리
    public JwtUserClaim parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return this.convert(claims);
    }
}
