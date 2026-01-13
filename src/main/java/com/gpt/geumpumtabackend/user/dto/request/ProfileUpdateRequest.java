package com.gpt.geumpumtabackend.user.dto.request;

import jakarta.validation.constraints.Pattern;

public record ProfileUpdateRequest(
        String imageUrl,
        String publicId,
        @Pattern(
                regexp = "^(?! )[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9 ]{2,11}(?<! )$",
                message = "닉네임은 한글, 영어, 숫자, 공백 포함 2~11자 이내여야 합니다."
        )
        String nickname
) {
}
