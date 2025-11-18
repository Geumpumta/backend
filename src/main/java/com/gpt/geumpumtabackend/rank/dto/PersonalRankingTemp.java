package com.gpt.geumpumtabackend.rank.dto;

import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.Getter;

@Getter
public class PersonalRankingTemp {
    private Long userId;
    private String username;
    private Long totalMillis;
    private Long ranking;
    private String imageUrl;
    private String department;

    public PersonalRankingTemp(Long userId, String username, Long totalMillis, Long ranking, String imageUrl, String department) {
        this.userId = userId;
        this.username = username;
        this.totalMillis = totalMillis;
        this.ranking = ranking;
        this.imageUrl = imageUrl;
        this.department = department;
    }
    
    // JPQL에서 Department enum을 받는 생성자
    public PersonalRankingTemp(Long userId, String username, Long totalMillis, Long ranking, String imageUrl, Department department) {
        this.userId = userId;
        this.username = username;
        this.totalMillis = totalMillis;
        this.ranking = ranking;
        this.imageUrl = imageUrl;
        this.department = department != null ? department.getKoreanName() : null;
    }
}
