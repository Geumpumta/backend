package com.gpt.geumpumtabackend.global.config.retry;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정
 * - @Retryable 어노테이션 활성화
 * - 스냅샷 생성 실패 시 자동 재시도 지원
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
