package com.gpt.geumpumtabackend.rank.dto.response;

import java.util.List;


public record PersonalRankingResponse(
        List<PersonalRankingEntryResponse> topRanks, PersonalRankingEntryResponse myRanking
) {

}

