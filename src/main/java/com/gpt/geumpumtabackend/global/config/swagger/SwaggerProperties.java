package com.gpt.geumpumtabackend.global.config.swagger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "geumpumta.swagger")
public class SwaggerProperties {
    private String serverUrl;
    private String description;
}