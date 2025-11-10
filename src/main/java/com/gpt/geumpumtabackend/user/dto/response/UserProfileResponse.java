package com.gpt.geumpumtabackend.user.dto.response;

import com.gpt.geumpumtabackend.user.domain.User;

public record UserProfileResponse(
        String email,
        String schoolEmail,
        String userRole,
        String name,
        String nickName,
        String profilePictureUrl,
        String OAuthProvider,
        String studentId,
        String department
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getSchoolEmail(),
                user.getRole().toString(),
                user.getName(),
                user.getNickname(),
                user.getPicture(), user.getProvider().toString(),
                user.getStudentId(),
                user.getDepartment().getKoreanName()
        );
    }
}
