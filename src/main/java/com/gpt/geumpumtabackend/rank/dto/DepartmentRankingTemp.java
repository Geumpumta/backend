package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class DepartmentRankingTemp {
    private String departmentName;
    private Long totalMillis;
    private Long ranking;


    public DepartmentRankingTemp(Department department, Long totalMillis, Long ranking) {
        this.departmentName = department.getKoreanName();
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }
}
