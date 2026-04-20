package com.gpt.geumpumtabackend.maintenance.domain;

import com.gpt.geumpumtabackend.global.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Maintenance extends BaseEntity {

    public static final Long DEFAULT_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status;

    @Column(length = 255)
    private String message;

    private Maintenance(Long id, ServiceStatus status, String message) {
        this.id = id;
        this.status = status;
        this.message = message;
    }

    public static Maintenance initialize(ServiceStatus status, String message) {
        return new Maintenance(DEFAULT_ID, status, message);
    }

    public void update(ServiceStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
