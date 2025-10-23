package com.gpt.geumpumtabackend.user.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum UserRole {

    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String key;
}