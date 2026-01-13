package com.gpt.geumpumtabackend.user.dto.response;

public record NicknameVerifyResponse(
        boolean isAvailable
) {
    public static NicknameVerifyResponse of(boolean isAvailable){
        return new NicknameVerifyResponse(isAvailable);
    }
}
