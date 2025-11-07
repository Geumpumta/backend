package com.gpt.geumpumtabackend.rank.dto.response;


import java.util.List;

public record DepartmentRankingResponse(
        List<DepartmentRankingEntryResponse> topRanks,
        DepartmentRankingEntryResponse myDepartmentRanking)
{ }
