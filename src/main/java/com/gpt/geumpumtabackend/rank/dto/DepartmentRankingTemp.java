package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class DepartmentRankingTemp {
    private String department; // 원본 enum 값 저장
    private Long totalMillis;
    private Long ranking;

    public DepartmentRankingTemp(String department, Long totalMillis, Long ranking) {
        this.department = department; // 원본값 그대로 저장
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }
    
    // Department enum 값을 한국어로 변환하는 메서드 
    public String getDepartmentName() {
        if (department == null) return null;
        
        try {
            Department dept = Department.valueOf(department);
            return dept.getKoreanName();
        } catch (IllegalArgumentException e) {
            return department; // enum에 없는 값이면 그대로 반환
        }
    }
}
