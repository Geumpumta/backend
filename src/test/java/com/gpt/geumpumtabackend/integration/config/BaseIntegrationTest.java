package com.gpt.geumpumtabackend.integration.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Base class for integration tests using TestContainers.
 *
 * Configuration approach:
 * - Uses programmatic TestContainers management (@Container)
 * - Containers are shared across all test classes (static)
 * - Container reuse enabled for faster test execution
 * - @DynamicPropertySource overrides application-test.yml datasource settings
 */
@SpringBootTest(
        properties = {
                "spring.test.database.replace=NONE",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_geumpumta")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withCommand("--default-authentication-plugin=mysql_native_password");

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL 설정
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        
        // Redis 설정
        registry.add("spring.data.redis.host", () -> redisContainer.getHost());
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");
        
        // WiFi 검증을 위한 테스트 설정
        registry.add("campus-wifi.networks[0].ssid", () -> "KUMOH_TEST");
        registry.add("campus-wifi.networks[0].gateway-ip", () -> "192.168.1.1");
        registry.add("campus-wifi.networks[0].ip-ranges[0]", () -> "192.168.1.0/24");
        registry.add("campus-wifi.networks[0].active", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void cleanUp() {
        truncateAllTables();
        cleanRedisCache();
    }

    private void truncateAllTables() {
        try {
            // Connection을 try-with-resources로 자동 close
            String dbProductName;
            try (var connection = jdbcTemplate.getDataSource().getConnection()) {
                dbProductName = connection.getMetaData().getDatabaseProductName();
            }
            boolean isH2 = "H2".equalsIgnoreCase(dbProductName);

            // 외래 키 제약 조건 비활성화
            if (isH2) {
                jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
            } else {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            }

            List<String> tableNames;
            if (isH2) {
                tableNames = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                    String.class
                );
            } else {
                tableNames = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'",
                    String.class
                );
            }

            for (String tableName : tableNames) {
                jdbcTemplate.execute("TRUNCATE TABLE `" + tableName + "`");
            }

            // 외래 키 제약 조건 재활성화
            if (isH2) {
                jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            } else {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to truncate tables", e);
        }
    }

    private void cleanRedisCache() {
        // Redis의 모든 캐시 데이터 삭제 (Connection을 try-with-resources로 자동 close)
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushAll();
        }
    }
}