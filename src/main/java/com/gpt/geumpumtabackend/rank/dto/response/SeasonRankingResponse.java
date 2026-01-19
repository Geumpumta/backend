package com.gpt.geumpumtabackend.rank.dto.response;

import com.gpt.geumpumtabackend.rank.domain.Season;
import com.gpt.geumpumtabackend.rank.dto.PersonalRankingTemp;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


public record SeasonRankingResponse(
    Long seasonId,
    String seasonName,
    LocalDate startDate,
    LocalDate endDate,
    List<PersonalRankingEntryResponse> rankings
) {

    public static SeasonRankingResponse of(Season season, List<PersonalRankingTemp> rankings) {
        List<PersonalRankingTemp> safeRankings = (rankings == null) ? List.of() : rankings;
        List<PersonalRankingEntryResponse> rankingEntries = safeRankings.stream()
            .map(PersonalRankingEntryResponse::of)
            .collect(Collectors.toList());

        return new SeasonRankingResponse(
            season.getId(),
            season.getName(),
            season.getStartDate(),
            season.getEndDate(),
            rankingEntries
        );
    }
}
