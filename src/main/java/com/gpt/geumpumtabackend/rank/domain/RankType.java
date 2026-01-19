package com.gpt.geumpumtabackend.rank.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum RankType {


    OVERALL("전체"),

    DEPARTMENT("학과별");

    private final String displayName;
}
