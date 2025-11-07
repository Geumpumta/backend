package com.gpt.geumpumtabackend.rank.dto.response;

import com.gpt.geumpumtabackend.rank.dto.UserRankingTemp;
import com.gpt.geumpumtabackend.study.repository.StudySessionRepository.UserRankingProjection;

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

    public static PersonalRankingEntryResponse from(UserRankingProjection projection) {
        return new PersonalRankingEntryResponse(
                projection.getUserId(),
                projection.getTotalMillis(),
                projection.getRanking(),
                projection.getUsername()
        );
    }
}
