package com.gpt.geumpumtabackend.study.domain;

import com.gpt.geumpumtabackend.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long totalMillis;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StudyStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    private LocalDateTime heartBeatAt;

    public void startStudySession(LocalDateTime startTime, User user) {
        this.startTime = startTime;
        this.user = user;
        status = StudyStatus.STARTED;
    }

    public void endStudySession(LocalDateTime endTime) {
        this.endTime = endTime;
        status = StudyStatus.FINISHED;
        this.totalMillis = Duration.between(this.startTime, this.endTime).toMillis();
    }

    public void updateHeartBeatAt(LocalDateTime heartBeatAt) {
        this.heartBeatAt = heartBeatAt;
    }

    public void endByFocusTimeLimit(long maxFocusTime) {
        this.totalMillis = maxFocusTime;
    }
}
