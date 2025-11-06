package com.gpt.geumpumtabackend.rank.dto.response;

import com.gpt.geumpumtabackend.rank.dto.DepartmentRankingTemp;
import com.gpt.geumpumtabackend.user.domain.Department;

public record DepartmentRankingEntryResponse(
        Department departmentName,
        Long totalMillis,
        Integer rank
) {
    public static DepartmentRankingEntryResponse of(DepartmentRankingTemp departmentRankingTemp) {
        return new DepartmentRankingEntryResponse(
                departmentRankingTemp.getDepartmentName(),
                departmentRankingTemp.getTotalMillis(),
                departmentRankingTemp.getRank()
        );
    }
}
