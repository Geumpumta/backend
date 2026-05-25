package com.gpt.geumpumtabackend.global.jwt;


import com.gpt.geumpumtabackend.user.domain.User;
import com.gpt.geumpumtabackend.user.domain.UserRole;

public record  JwtUserClaim(
        Long userId,
        String sessionId,
        UserRole role,
        Boolean withdrawn
) {
    public JwtUserClaim(Long userId, UserRole role, Boolean withdrawn) {
        this(userId, null, role, withdrawn);
    }

    public static JwtUserClaim create(User user) {
        return new JwtUserClaim(user.getId(), null, user.getRole(), user.getDeletedAt() != null);
    }
    public static JwtUserClaim create(Long userId, UserRole role, Boolean withdrawn) {
        return new JwtUserClaim(userId, null, role, withdrawn);
    }
    public static JwtUserClaim create(User user, String sessionId) {
        return new JwtUserClaim(user.getId(), sessionId, user.getRole(), user.getDeletedAt() != null);
    }
    public static JwtUserClaim create(Long userId, String sessionId, UserRole role, Boolean withdrawn) {
        return new JwtUserClaim(userId, sessionId, role, withdrawn);
    }
}
