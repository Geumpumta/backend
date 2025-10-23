package com.gpt.geumpumtabackend.global.jwt;


import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;

public record  JwtUserClaim(
        Long userId,
        UserRole role
) {
    public static JwtUserClaim create(User user) {
        return new JwtUserClaim(user.getId(), user.getRole());
    }
    public static JwtUserClaim create(Long userId, UserRole role) {
        return new JwtUserClaim(userId, role);
    }
}
