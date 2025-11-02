package com.gpt.geumpumtabackend.study.domain;


import lombok.Getter;

@Getter
public enum StudyStatus {

    STARTED("진행중"), FINISHED("완료");


    private final String status; // 왜 final?

    StudyStatus(String status) {
        this.status = status;
    }
}
