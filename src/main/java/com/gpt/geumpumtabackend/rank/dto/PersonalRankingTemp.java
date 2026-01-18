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

    public PersonalRankingTemp(Long userId, String nickname, String imageUrl, String department, Long totalMillis, Long ranking) {
        this.userId = userId;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.department = department; // 원본값 그대로 저장
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }

    // JPQL에서 Department enum을 직접 전달받는 생성자
    public PersonalRankingTemp(Long userId, String nickname, String imageUrl, Department department, Long totalMillis, Long ranking) {
        this.userId = userId;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.department = department != null ? department.name() : null;
        this.totalMillis = totalMillis;
        this.ranking = ranking;
    }

    // JPQL 리터럴 0L이 int로 추론될 때를 위한 생성자
    public PersonalRankingTemp(Long userId, String nickname, String imageUrl, Department department, Long totalMillis, int ranking) {
        this.userId = userId;
        this.nickname = nickname;
        this.imageUrl = imageUrl;
        this.department = department != null ? department.name() : null;
        this.totalMillis = totalMillis;
        this.ranking = (long) ranking;
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
