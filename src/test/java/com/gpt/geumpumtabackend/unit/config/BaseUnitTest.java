package com.gpt.geumpumtabackend.unit.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("unit-test")
public abstract class BaseUnitTest {
    // 단위테스트 기본 설정
    // - H2 Database
    // - Redis 완전 비활성화
    // - Mock 기반 테스트
}