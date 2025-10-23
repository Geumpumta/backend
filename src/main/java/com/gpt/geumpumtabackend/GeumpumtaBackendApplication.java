package com.gpt.geumpumtabackend;

import com.gpt.geumpumtabackend.global.oauth.user.KakaoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
@EnableConfigurationProperties({KakaoProperties.class})
public class GeumpumtaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeumpumtaBackendApplication.class, args);
    }

}
