package com.gpt.geumpumtabackend.rank.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum SeasonStatus {


    ACTIVE("진행중"),
    ENDED("종료");

    private final String displayName;
}
