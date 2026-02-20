package com.gpt.geumpumtabackend.user.dto.response;

import com.gpt.geumpumtabackend.badge.dto.response.NewBadgeResponse;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;

import java.util.List;

public record CompleteRegistrationResponse(
        TokenResponse token,
        NewBadgeResponse newBadge
) {
    public static CompleteRegistrationResponse of(TokenResponse token, NewBadgeResponse newBadge) {
        return new CompleteRegistrationResponse(token, newBadge);
    }
}
