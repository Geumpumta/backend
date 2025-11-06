package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class DepartmentRankingTemp {
    private Department departmentName;
    private Long totalMillis;
    private Integer rank;

    public DepartmentRankingTemp(Department departmentName, Long totalMillis, Integer rank) {
        this.departmentName = departmentName;
        this.totalMillis = totalMillis;
        this.rank = rank;
    }
}
