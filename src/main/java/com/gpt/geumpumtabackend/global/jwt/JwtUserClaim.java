package com.gpt.geumpumtabackend.global.jwt;


import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;

public record  JwtUserClaim(
        Long userId,
        UserRole role,
        Boolean withdrawn
) {
    public static JwtUserClaim create(User user) {
        return new JwtUserClaim(user.getId(), user.getRole(), user.getDeletedAt() == null);
    }
    public static JwtUserClaim create(Long userId, UserRole role, Boolean withdrawn) {
        return new JwtUserClaim(userId, role, withdrawn);
    }
}
