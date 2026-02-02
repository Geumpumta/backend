# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Geumpumta (열정품은타이머)** — 금오공과대학교 학생 대상 공부 시간 관리 애플리케이션 백엔드. Spring Boot 기반으로, 학습 세션 추적, 대학 인증(캠퍼스 Wi-Fi + 이메일), 시즌/랭킹 시스템을 제공한다.

## Build & Run

```bash
# 인프라 실행 (MySQL 8.4, Redis)
docker-compose up -d

# 빌드
./gradlew clean build

# 로컬 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 테스트
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "ClassName"
```

## Tech Stack

- **Java 21**, **Spring Boot 3.5.6**, **Gradle**
- **Spring Security** + OAuth2 Client (Kakao, Google, Apple)
- **Spring Data JPA** + MySQL 8
- **Spring Data Redis** + Caffeine Cache
- **JWT**: JJWT 0.12.6 + Nimbus JOSE JWT 9.37.4
- **API Docs**: SpringDoc OpenAPI 2.8.14
- **Image**: Cloudinary 1.39.0
- **Network**: Apache Commons Net 3.11.1 (IP range validation)
- **Retry**: Spring Retry + Spring Aspects
- **Test**: JUnit 5, Mockito, TestContainers (MySQL)

## Project Structure

```
src/main/java/com/gpt/geumpumtabackend/
├── GeumpumtaBackendApplication.java
│
├── global/                    # 공통 인프라
│   ├── aop/                   # @AssignUserId AOP
│   ├── base/                  # BaseEntity (createdAt, updatedAt, deletedAt)
│   ├── config/                # cache, image, mail, redis, retry, security, swagger
│   ├── exception/             # GlobalExceptionHandler, BusinessException, ExceptionType
│   ├── jwt/                   # JwtHandler, TokenProvider, JwtAuthenticationFilter
│   ├── oauth/                 # OAuth2 (Kakao/Google/Apple), handlers, resolvers
│   ├── response/              # ResponseUtil, ResponseBody, GlobalPageResponse
│   └── scheduler/             # RefreshTokenDeleteScheduler
│
├── board/                     # 게시판
├── image/                     # 이미지 업로드 (Cloudinary)
├── rank/                      # 랭킹 시스템
│   ├── domain/                # UserRanking, DepartmentRanking, Season, SeasonRankingSnapshot
│   ├── scheduler/             # RankingSchedulerService, SeasonTransitionScheduler
│   └── service/               # Personal/Department/SeasonRank, SeasonSnapshot services
├── statistics/                # 통계 (일간/주간/월간, 잔디)
├── study/                     # 학습 세션 (시작/종료, Wi-Fi 검증)
├── token/                     # JWT 토큰 관리 (발급/갱신)
├── user/                      # 사용자, 이메일 인증, 프로필
└── wifi/                      # 캠퍼스 Wi-Fi 검증
```

각 도메인 모듈은 `api/ → controller/ → service/ → repository/ → domain/ → dto/` 계층 구조를 따른다.

## Architecture Patterns

### Layered Architecture
`Controller → Service → Repository → Entity` 순서. Controller는 HTTP 처리만, Service에 비즈니스 로직 집중.

### AOP User Context Injection
```java
@PreAuthorize("isAuthenticated() and hasRole('USER')")
@AssignUserId  // JWT에서 userId 자동 주입
public ResponseEntity<T> endpoint(Long userId) { ... }
```

### Standardized Response
```java
ResponseUtil.createSuccessResponse(data);
ResponseUtil.createFailureResponse(ExceptionType.ERROR_TYPE);
```

### Exception Handling
- `GlobalExceptionHandler` (`@RestControllerAdvice`)에서 전역 처리
- `ExceptionType` enum으로 에러 코드/메시지 관리
- 도메인 예외는 `BusinessException` 상속

### Soft Delete
모든 엔티티가 `BaseEntity`를 상속 → `createdAt`, `updatedAt`, `deletedAt` 필드 자동 관리.

## Configuration

### Profiles
| Profile | DB | DDL Mode | 용도 |
|---------|-----|----------|------|
| `local` | MySQL localhost:3311 | create-drop | 로컬 개발 |
| `dev` | Docker MySQL | update | 개발 서버 |
| `prod` | Production DB | validate | 운영 |
| `test` | TestContainers MySQL | - | 통합 테스트 |
| `unit-test` | H2 | - | 단위 테스트 |

### Sensitive Config (Git Submodule)
`src/main/resources/security/` 디렉토리에 민감한 설정 파일들이 git submodule로 관리됨. 절대 직접 커밋하지 않는다.
- `application-database.yml`, `application-security.yml`, `application-mail.yml`
- `application-swagger.yml`, `application-wifi.yml`, `application-cloudinary.yml`

## Key Business Logic

### Study Session
1. **시작**: Wi-Fi 검증 → `StudySession` 생성 (서버 타임스탬프, `STARTED`)
2. **종료**: 서버에서 `endTime` 계산, `FINISHED` 상태, `Duration.between().toMillis()`로 시간 산출
3. 클라이언트 타임스탬프 사용하지 않음 — 모든 시간은 서버에서 관리

### Ranking System
- **UserRanking**: 개인 공부 시간 랭킹
- **DepartmentRanking**: 학과별 집계 랭킹
- **RankingType**: DAILY, WEEKLY, MONTHLY
- **Season**: 시즌제 운영, `SeasonRankingSnapshot`으로 이력 관리
- `RankingSchedulerService`가 주기적으로 랭킹 재계산

### Wi-Fi Validation
캠퍼스 Wi-Fi gateway IP/client IP 검증. Redis 캐싱 적용.

### Authentication Flow
OAuth2 로그인 (Kakao/Google/Apple) → 대학 이메일 인증 (@kumoh.ac.kr) → JWT 발급 (14일)

## Testing

### Unit Tests (`src/test/java/.../unit/`)
- JUnit 5 + Mockito + AssertJ
- `BaseUnitTest` 기반
- 대상: StudySession 시간 계산, 랭킹 로직, Wi-Fi 검증, 시즌 서비스

### Integration Tests (`src/test/java/.../integration/`)
- TestContainers MySQL 8.0 (`withReuse(true)`)
- `BaseIntegrationTest` 기반, TRUNCATE로 데이터 초기화
- 대상: Controller 엔드투엔드 테스트

## CI/CD

### GitHub Actions
- **CI**: Java 21 빌드, Redis 서비스 연동, 테스트, 아티팩트 업로드
- **CD**: Docker 멀티 아키텍처 빌드 (AMD64/ARM64), GHCR 푸시, self-hosted 배포

### Docker
- Base image: `amd64/openjdk:21-jdk-slim`
- `docker-compose.yml`: MySQL 8.4.0 + Redis Alpine (로컬 개발용)

## URL Pattern

```
/api/v1/{domain}/*
```

## Development Checklist

1. 도메인 모듈 구조 준수 (`api/ → controller/ → service/ → repository/ → domain/ → dto/`)
2. 인증 필요한 엔드포인트에 `@AssignUserId` 사용
3. `@Transactional` 적절히 적용
4. `ResponseUtil`로 응답 표준화
5. 새 에러는 `ExceptionType` enum에 추가
6. `security/` 디렉토리 파일 커밋 금지
