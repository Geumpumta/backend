package com.gpt.geumpumtabackend.maintenance.domain;

import lombok.Getter;

@Getter
public enum ServiceStatus {
    NORMAL("정상"),
    MAINTENANCE("점검중");

    private final String status;

    ServiceStatus(String status) {
        this.status = status;
    }
}
