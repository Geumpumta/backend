package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class DepartmentRankingTemp {
    private String departmentName;
    private Long totalMillis;
    private Long ranking;

    public DepartmentRankingTemp(String departmentName, Long totalMillis, Long ranking) {
        this.departmentName = departmentName;
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }
}
