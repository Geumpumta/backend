package com.gpt.geumpumtabackend.rank.dto.response;

import com.gpt.geumpumtabackend.rank.dto.UserRankingTemp;

//TODO : 학과 추가
public record PersonalRankingEntryResponse(
        Long userId,
        Long totalMillis,
        Long rank,
        String username
) {
    public static PersonalRankingEntryResponse of(UserRankingTemp userRankingTemp) {
        return new PersonalRankingEntryResponse(
                userRankingTemp.getUserId(),
                userRankingTemp.getTotalMillis(),
                userRankingTemp.getRank(),
                userRankingTemp.getUsername()
        );
    }
}
