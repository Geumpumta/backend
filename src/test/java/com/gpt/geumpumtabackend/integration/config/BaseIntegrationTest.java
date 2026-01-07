package com.gpt.geumpumtabackend.integration.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

/**
 * Base class for integration tests using TestContainers.
 *
 * Configuration approach:
 * - Uses programmatic TestContainers management (@Container)
 * - Containers are shared across all test classes (static)
 * - Container reuse enabled for faster local development
 * - Startup timeout increased for CI environments (90s for MySQL, 60s for Redis)
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
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_geumpumta")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                "--default-authentication-plugin=mysql_native_password",
                "--max_connections=500",
                "--wait_timeout=28800"
            )
            .withStartupTimeout(Duration.ofSeconds(120))
            .withReuse(false)  // CI 환경에서는 재사용 비활성화
            .waitingFor(org.testcontainers.containers.wait.strategy.Wait
                .forLogMessage(".*ready for connections.*\\n", 2));  // MySQL이 2번 ready 메시지 출력할 때까지 대기

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofSeconds(90))
            .withReuse(false)  // CI 환경에서는 재사용 비활성화
            .waitingFor(org.testcontainers.containers.wait.strategy.Wait
                .forLogMessage(".*Ready to accept connections.*\\n", 1));

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
        registry.add("campus.wifi.networks[0].name", () -> "KUMOH_TEST");
        registry.add("campus.wifi.networks[0].gateway-ips[0]", () -> "172.30.64.1");
        registry.add("campus.wifi.networks[0].ip-ranges[0]", () -> "172.30.64.0/18");
        registry.add("campus.wifi.networks[0].active", () -> "true");
        registry.add("campus.wifi.networks[0].description", () -> "Test Network");
        registry.add("campus.wifi.validation.cache-ttl-minutes", () -> "5");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void cleanUp() {
        try {
            truncateAllTables();
            cleanRedisCache();
        } catch (Exception e) {
            // 테스트 실패 시 cleanup도 실패할 수 있으므로 무시
            System.err.println("Cleanup failed, but continuing: " + e.getMessage());
        }
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
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
                    connection.serverCommands().flushAll();
                }
            }
        } catch (Exception e) {
            System.err.println("Redis cleanup failed: " + e.getMessage());
        }
    }
}