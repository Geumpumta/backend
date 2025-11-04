package com.gpt.geumpumtabackend.user.dto.response;

public record EmailCodeVerifyResponse(
        boolean isVerified
) {
    public static EmailCodeVerifyResponse of(boolean isVerified){
        return new EmailCodeVerifyResponse(isVerified);
    }
}
