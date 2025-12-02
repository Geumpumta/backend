package com.gpt.geumpumtabackend.global.jwt;

import com.gpt.geumpumtabackend.user.domain.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public record JwtAuthentication(
        Long userId,
        UserRole role,
        Boolean withdrawn
) implements Authentication {

    public JwtAuthentication(JwtUserClaim claims) {
        this(
                claims.userId(),
                claims.role(),
                claims.withdrawn()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(this.role().getKey()));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public boolean isWithdrawn() {
        return withdrawn;
    }
}