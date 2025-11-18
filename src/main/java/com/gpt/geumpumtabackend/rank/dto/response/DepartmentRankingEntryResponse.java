package com.gpt.geumpumtabackend.rank.dto.response;

import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;


public record DepartmentRankingEntryResponse(
        String departmentName,
        Long totalMillis,
        Long rank
) {
    public static DepartmentRankingEntryResponse of(DepartmentRankingTemp departmentRankingTemp) {
        return new DepartmentRankingEntryResponse(
                departmentRankingTemp.getDepartmentName(),
                departmentRankingTemp.getTotalMillis(),
                departmentRankingTemp.getRanking()
        );
    }
}
