package com.gpt.geumpumtabackend.study.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "study")
@Getter
@Setter
public class StudyProperties {
    /**
     * 최대 집중 공부 시간 (시간 단위)
     */
    private int maxFocusHours = 3;
}
