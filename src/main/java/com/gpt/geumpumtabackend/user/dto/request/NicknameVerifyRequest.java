package com.gpt.geumpumtabackend.user.dto.request;

import jakarta.validation.constraints.Pattern;

public record NicknameVerifyRequest (
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,11}$", message = "닉네임은 한글, 영어, 숫자로 2~11자 이내여야 합니다.")
        String nickname
){
}
