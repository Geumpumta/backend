package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class DepartmentRankingTemp {
    private String departmentName;
    private Long totalMillis;
    private Long ranking;

    // JPA 쿼리에서 Department enum을 받기 위한 추가 생성자
    public DepartmentRankingTemp(Department department, Long totalMillis, Long ranking) {
        this.departmentName = department.name(); // enum을 String으로 변환
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }
}
