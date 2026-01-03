package com.gpt.geumpumtabackend.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

@TestConfiguration
public class TestContainerConfig {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_geumpumta")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // 컨테이너 재사용으로 성능 향상

    static {
        mysqlContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQL8Dialect");
        registry.add("spring.jpa.show-sql", () -> "false");
        
        // Redis 비활성화 (통합테스트에서는 사용하지 않음)
        registry.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Bean
    public MySQLContainer<?> mySQLContainer() {
        return mysqlContainer;
    }
}