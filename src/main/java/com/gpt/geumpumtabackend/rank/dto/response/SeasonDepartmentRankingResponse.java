package com.gpt.geumpumtabackend.rank.dto.response;

import com.gpt.geumpumtabackend.rank.domain.Season;
import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


public record SeasonDepartmentRankingResponse(
    Long seasonId,
    String seasonName,
    LocalDate startDate,
    LocalDate endDate,
    List<DepartmentRankingEntryResponse> topRanks,
    DepartmentRankingEntryResponse myDepartmentRanking
) {

    public static SeasonDepartmentRankingResponse of(
            Season season,
            List<DepartmentRankingEntryResponse> topRanks,
            DepartmentRankingEntryResponse myDepartmentRanking
    ) {
        return new SeasonDepartmentRankingResponse(
            season.getId(),
            season.getName(),
            season.getStartDate(),
            season.getEndDate(),
            topRanks,
            myDepartmentRanking
        );
    }
}
