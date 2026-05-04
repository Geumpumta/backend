---
name: spring-core
description: "Spring Boot + Java 백엔드 구현. API 추가, 엔티티, DTO, 서비스,
  Repository, 예외 처리, 인증, Swagger 문서화. 'API 만들어줘', '엔티티 추가',
  '새 도메인', 'DTO 작성', '에러 코드 추가', '컨트롤러 작성' 요청 시 트리거."
---

# Spring Core — Geumpumta 백엔드 구현 가이드

Layered Architecture 기반. 모든 도메인이 동일한 구조와 패턴을 따른다.

> 코드 템플릿이 필요하면 `references/templates.md`를 읽어라.
> 예외 처리 접두사·규칙은 `references/exceptions.md`를 읽어라.

---

## 도메인 모듈 구조

```
{도메인명}/
├── api/{도메인}Api.java           # Swagger 문서 인터페이스
├── controller/{도메인}Controller.java  # HTTP 매핑만 (로직 없음)
├── service/{도메인}Service.java        # 비즈니스 로직
├── repository/{도메인}Repository.java  # JPA Repository
├── domain/{도메인}.java               # 엔티티, Enum
└── dto/                               # record 기반 DTO
    ├── request/  (선택)
    └── response/ (선택)
```

---

## 핵심 패턴 요약

| 레이어 | 패턴 |
|--------|------|
| **엔티티** | `BaseEntity` 상속, `@Getter`, `@NoArgsConstructor`, `@Builder`(생성자), `@Setter` 금지 |
| **DTO** | Java `record`, `from()` 정적 팩토리, 요청에 Jakarta Validation |
| **Repository** | `JpaRepository<Entity, Long>` 상속 |
| **Service** | 클래스 `@Transactional(readOnly=true)`, 쓰기만 `@Transactional` |
| **Controller** | `@AssignUserId` + `@PreAuthorize`, `ResponseUtil.createSuccessResponse()` |
| **Swagger** | Api 인터페이스 분리, `@Parameter(hidden=true)` userId |
| **예외** | `BusinessException(ExceptionType.XXX)`, GlobalExceptionHandler 자동 처리 |

---

## 인증/인가

```java
// 일반 사용자
@AssignUserId
@PreAuthorize("isAuthenticated() and hasRole('USER')")

// 관리자 전용
@AssignUserId
@PreAuthorize("isAuthenticated() and hasRole('ADMIN')")

// 선택적 userId
@AssignUserId(required = false)

// 공개 API — 어노테이션 생략, Security 설정에 permitAll() 추가
```

---

## 응답 형식

```java
// 데이터 있음
return ResponseEntity.ok(ResponseUtil.createSuccessResponse(data));
// 데이터 없음 (DELETE 등)
return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
```

실패 응답은 `GlobalExceptionHandler`가 자동 처리한다.

---

## 새 도메인 추가 체크리스트

1. 도메인 패키지 생성 (위 구조)
2. 엔티티 — `BaseEntity` 상속, `@Builder` 생성자
3. 에러 코드 — `ExceptionType` enum에 새 접두사 + 코드 (`references/exceptions.md` 참고)
4. Repository — `JpaRepository` 상속
5. DTO — `record`, `from()` 팩토리
6. Service — `@Transactional(readOnly=true)`, `@RequiredArgsConstructor`
7. Swagger Api 인터페이스
8. Controller — Api 구현, `@AssignUserId` + `@PreAuthorize`
9. 테스트 — 단위(`unit/`) + 통합(`integration/`)

---

## 절대 금지

1. **클라이언트 타임스탬프 신뢰 금지** — 시간은 서버에서만 생성
2. **`security/` 디렉토리 파일 커밋 금지** — git submodule
3. **`StudySessionRepository` Native Query 수정 시 랭킹/통계 영향 확인**
4. **시즌/캐시 코드 수정 시 `activeSeason` 캐시 eviction 확인**

---

## 테스트 패턴

- **단위** (`src/test/.../unit/`): `BaseUnitTest` 상속, Mockito + AssertJ, 프로파일 `unit-test`
- **통합** (`src/test/.../integration/`): `BaseIntegrationTest` 상속, TestContainers MySQL 8.0, 프로파일 `test`
