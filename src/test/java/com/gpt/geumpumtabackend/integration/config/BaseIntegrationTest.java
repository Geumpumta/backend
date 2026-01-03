package com.gpt.geumpumtabackend.integration.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(TestContainerConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        truncateAllTables();
    }

    private void truncateAllTables() {
        // 외래 키 제약 조건 비활성화
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");

        // 모든 테이블 목록 조회 및 TRUNCATE
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'", 
            String.class
        );

        for (String tableName : tableNames) {
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName);
        }

        // 외래 키 제약 조건 재활성화
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}