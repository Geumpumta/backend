package com.gpt.geumpumtabackend.rank.dto.response;


//TODO : 학과 추가
public record PersonalRankingEntryResponse(
        Long userId,
        Long totalMillis,
        Long rank,
        String username
) {
    public static PersonalRankingEntryResponse of(com.gpt.geumpumtabackend.rank.dto.UserRankingTemp userRankingTemp) {
        return new PersonalRankingEntryResponse(
                userRankingTemp.getUserId(),
                userRankingTemp.getTotalMillis(),
                userRankingTemp.getRanking(),
                userRankingTemp.getUsername()
        );
    }


}
