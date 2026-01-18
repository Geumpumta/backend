package com.gpt.geumpumtabackend.rank.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시즌 타입
 * - 학기와 방학을 구분하여 4개 시즌 운영
 */
@Getter
@RequiredArgsConstructor
public enum SeasonType {


    SPRING_SEMESTER("1학기"),

    SUMMER_VACATION("여름방학"),

    FALL_SEMESTER("2학기"),

    WINTER_VACATION("겨울방학");

    private final String displayName;
}
