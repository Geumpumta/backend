package com.gpt.geumpumtabackend.user.dto.response;

import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;

public record CompleteRegistrationResponse(
        TokenResponse token
) {
    public static CompleteRegistrationResponse of(TokenResponse token) {
        return new CompleteRegistrationResponse(token);
    }
}
