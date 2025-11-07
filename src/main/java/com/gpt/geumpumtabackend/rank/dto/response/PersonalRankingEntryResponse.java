package com.gpt.geumpumtabackend.rank.dto.response;


import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;

//TODO : 학과 추가
public record PersonalRankingEntryResponse(
        Long userId,
        Long totalMillis,
        Long rank,
        String username
) {
    public static PersonalRankingEntryResponse of(PersonalRankingTemp userRankingTemp) {
        return new PersonalRankingEntryResponse(
                userRankingTemp.getUserId(),
                userRankingTemp.getTotalMillis(),
                userRankingTemp.getRanking(),
                userRankingTemp.getUsername()
        );
    }


}
