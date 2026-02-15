package com.gpt.geumpumtabackend.global.config.fcm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "firebase")
public class FcmProperties {
    private String serviceAccountPath;
    private String projectId;
}
