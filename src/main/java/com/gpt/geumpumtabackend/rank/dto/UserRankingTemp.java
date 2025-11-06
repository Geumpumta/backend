package com.gpt.geumpumtabackend.rank.dto;

import lombok.Getter;

@Getter
public class UserRankingTemp {
    private Long userId;
    private String username;
    private Long totalMillis;
    private Long rank;

    public UserRankingTemp(Long userId, String username, Long totalMillis, Long rank) {
        this.userId = userId;
        this.username = username;
        this.totalMillis = totalMillis;
        this.rank = rank;
    }
}
