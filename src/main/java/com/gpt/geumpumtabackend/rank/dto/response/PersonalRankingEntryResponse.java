package com.gpt.geumpumtabackend.rank.dto.response;


import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;

public record PersonalRankingEntryResponse(
        Long userId,
        Long totalMillis,
        Long rank,
        String username,
        String imageUrl,
        String department
) {
    public static PersonalRankingEntryResponse of(PersonalRankingTemp userRankingTemp) {
        return new PersonalRankingEntryResponse(
                userRankingTemp.getUserId(),
                userRankingTemp.getTotalMillis(),
                userRankingTemp.getRanking(),
                userRankingTemp.getUsername(),
                userRankingTemp.getImageUrl(),
                userRankingTemp.getDepartmentKoreanName()
        );
    }


}
