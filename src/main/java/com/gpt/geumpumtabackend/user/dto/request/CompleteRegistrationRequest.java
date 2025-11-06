package com.gpt.geumpumtabackend.user.dto.request;

import jakarta.validation.constraints.Pattern;

public record CompleteRegistrationRequest(
        @Pattern(regexp = "^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@kumoh\\.ac\\.kr$", message = "학교 이메일을 입력하세요")
        String email,
        String studentId,
        String department
) {

}
