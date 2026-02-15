package com.gpt.geumpumtabackend.global.config.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FcmProperties.class)
@Slf4j
public class FcmConfig {

    private final FcmProperties fcmProperties;
    private final ResourceLoader resourceLoader;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            Resource resource = resourceLoader.getResource(fcmProperties.getServiceAccountPath());

            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(fcmProperties.getProjectId())
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                log.info("Firebase App initialized successfully: {}", app.getName());
                return app;
            }
        }

        log.info("Firebase App already initialized");
        return FirebaseApp.getInstance();
    }
}
