package com.gpt.geumpumtabackend.token.dto.response;


import com.gpt.geumpumtabackend.token.domain.Token;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
    public static TokenResponse to(Token token){
        return new TokenResponse(token.getAccessToken(), token.getRefreshToken());
    }
}
