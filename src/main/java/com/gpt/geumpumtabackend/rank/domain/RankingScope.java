package com.gpt.geumpumtabackend.rank.domain;

import lombok.Getter;

@Getter
public enum RankingScope {

    PERSONAL("개인"),DEPARTMENT("학과");

    private final String name;

    RankingScope(String name) {
        this.name = name;
    }
}
