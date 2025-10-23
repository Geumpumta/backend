package com.gpt.geumpumtabackend.global.config.security;


import com.gpt.geumpumtabackend.global.jwt.JwtHandler;
import com.gpt.geumpumtabackend.global.jwt.JwtProperties;
import com.gpt.geumpumtabackend.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private final RefreshTokenRepository refreshTokenRepository;

    @Bean
    public JwtHandler jwtHandler(JwtProperties jwtProperties) {
        return new JwtHandler(jwtProperties, refreshTokenRepository);
    }
}
