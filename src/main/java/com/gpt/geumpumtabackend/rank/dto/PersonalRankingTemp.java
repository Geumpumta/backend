package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class PersonalRankingTemp {
    private Long userId;
    private String nickname;
    private Long totalMillis;
    private Long ranking;
    private String imageUrl;
    private String department;

    public PersonalRankingTemp(Long userId, String nickname, String imageUrl, Object department, Long totalMillis, Long ranking) {
        this.userId = userId;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        if (department instanceof Department dept) {
            this.department = dept.name();
        } else if (department != null) {
            this.department = department.toString();
        }
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }

    // Department enum 값을 한국어로 변환하는 메서드
    public String getDepartmentKoreanName() {
        if (department == null) return null;
        
        try {
            Department dept = Department.valueOf(department);
            return dept.getKoreanName();
        } catch (IllegalArgumentException e) {
            return department; // enum에 없는 값이면 그대로 반환
        }
    }
}
