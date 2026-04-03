---
name: spring-review
description: >
  Geumpumta Spring Boot 백엔드 코드 리뷰 스킬.
  컨트롤러/서비스/레포지토리/엔티티/DTO 코드를 이 프로젝트의 아키텍처 규칙·보안·성능 기준으로 검토한다.
  사용자가 "코드 리뷰", "리뷰해줘", "코드 점검", "PR 리뷰", "코드 검토", "문제없어?", "이 코드 봐줘",
  "review", "check this code" 등을 요청할 때 반드시 이 스킬을 사용할 것.
  새 기능 구현 완료 후 검토를 요청하거나, PR 머지 전 확인을 요청할 때도 자동으로 트리거할 것.
---

# Spring Review — Geumpumta 코드 리뷰 가이드

코드를 건넸을 때 아래 항목 순서대로 검토하고, 발견된 문제를 **심각도(🔴 Critical / 🟡 Warning / 🔵 Suggestion)** 와 함께 보고한다.
문제가 없는 항목은 건너뛰고, 발견된 것만 명시한다. 마지막에 총평을 한 줄로 작성한다.

---

## 1. 레이어 아키텍처 위반 🔴

컨트롤러는 HTTP I/O만 담당한다. 다음이 있으면 즉시 지적:
- Controller에서 Repository를 직접 주입/호출
- Controller에서 비즈니스 로직 수행 (조건 분기, 계산 등)
- Service에서 ResponseEntity 반환 또는 HTTP 상태 코드 조작
- Service에서 다른 Service를 무분별하게 교차 호출 (순환 의존성 위험)

---

## 2. 인증·인가 누락 🔴

인증이 필요한 엔드포인트에 다음이 있는지 확인:
- `@PreAuthorize("isAuthenticated() and hasRole('USER')")` 누락
- `@AssignUserId` 누락 (userId를 파라미터로 받는 메서드에 필수)
- `userId`를 RequestParam/PathVariable로 클라이언트에서 직접 받는 경우 — **절대 금지**, JWT에서 추출해야 함

```java
// 올바른 패턴
@PreAuthorize("isAuthenticated() and hasRole('USER')")
@AssignUserId
public ResponseEntity<...> endpoint(Long userId) { ... }

// 잘못된 패턴 — userId를 클라이언트가 전달
public ResponseEntity<...> endpoint(@RequestParam Long userId) { ... }
```

---

## 3. 클라이언트 타임스탬프 신뢰 🔴

**모든 시간은 서버에서 생성한다.** 다음 패턴을 찾아 반드시 지적:
- `LocalDateTime` 또는 시간 관련 값을 Request DTO로 받는 경우
- `@RequestParam LocalDateTime` 으로 시간을 파라미터로 받는 경우
- 예외: `date` 파라미터 기반 과거 조회(랭킹/통계 이력 조회)는 허용

---

## 4. 예외 처리 패턴 🔴

- `throw new RuntimeException(...)` 또는 `throw new Exception(...)` 직접 사용 금지
  → `throw new BusinessException(ExceptionType.XXX)` 사용
- 새 에러 코드가 필요하면 `ExceptionType` enum에 추가 (접두사: `C` `S` `T` `U` `M` `ST` `W` `I` `B` `SE` `F`)
- `try-catch`에서 예외를 삼키는(swallow) 경우: 로깅 없이 빈 catch 블록

```java
// 잘못된 패턴
throw new RuntimeException("유저 없음");
catch (Exception e) { } // 예외 무시

// 올바른 패턴
throw new BusinessException(ExceptionType.USER_NOT_FOUND);
```

---

## 5. 응답 형식 🟡

- `ResponseEntity.ok(data)` 직접 반환 금지 → `ResponseUtil.createSuccessResponse(data)` 사용
- 빈 응답은 `ResponseUtil.createSuccessResponse()` (인자 없음)
- 실패 응답은 GlobalExceptionHandler가 처리하므로 Service/Controller에서 직접 만들지 않음

```java
// 올바른 패턴
return ResponseEntity.ok(ResponseUtil.createSuccessResponse(data));
return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
```

---

## 6. 트랜잭션 🟡

- 데이터 변경(INSERT/UPDATE/DELETE) 메서드에 `@Transactional` 누락 여부
- 읽기 전용 메서드에 `@Transactional(readOnly = true)` 누락 여부 (성능 최적화)
- Controller에 `@Transactional` 선언 — Service 레이어로 이동해야 함
- 트랜잭션 안에서 외부 API(FCM, Cloudinary 등) 호출 — 롤백 범위 문제 발생 가능

---

## 7. JPA·쿼리 🟡

- N+1 문제: 루프 안에서 Repository 호출, 또는 `@ManyToOne` 지연 로딩을 루프에서 참조
- Soft Delete: `BaseEntity`를 상속한 엔티티 삭제 시 `@SQLDelete` 미적용 또는 `deletedAt` 직접 조작
- `StudySessionRepository` Native Query 수정 시 → 랭킹/통계 도메인 영향 확인 요구 코멘트 추가
- `@Query`에서 `nativeQuery = true` 사용 시 MySQL 8+ 문법 의존성 명시

---

## 8. 입력 유효성 검사 🟡

- Request DTO에 `@Valid` 사용하는 Controller 메서드에서 `@Valid` 누락
- Request DTO 필드에 검증 어노테이션(`@NotNull`, `@NotBlank`, `@Size` 등) 누락
- 빈 문자열과 null을 구분해야 하는 필드에 `@NotBlank` 대신 `@NotNull` 사용

---

## 9. 보안 🔴

- SQL Injection: Native Query에서 `String` 파라미터를 문자열 연결로 조합 — `:param` 바인딩 사용
- 민감 정보(비밀번호, 토큰, 키) 로그 출력
- `security/` 디렉토리 파일 커밋 여부 확인 요청
- 스택 트레이스를 클라이언트에 직접 노출하는 응답

---

## 10. 캐시·시즌 🟡

- 시즌/캐시 관련 코드 수정 시 `activeSeason` 캐시 eviction 로직(`@CacheEvict`) 확인
- `@Cacheable` 사용 시 캐시 키 충돌 가능성 (userId, 기간 등 구분자 포함 여부)

---

## 리뷰 출력 형식

```
## 코드 리뷰: {파일명 또는 기능명}

### 🔴 Critical
1. **[레이어 위반]** `UserController`에서 `UserRepository`를 직접 주입하고 있습니다.
   → Service를 통해 데이터에 접근하세요.
   ```java
   // 현재 코드 (문제)
   // 수정 예시
   ```

### 🟡 Warning
1. **[트랜잭션 누락]** `createPost()` 메서드에 `@Transactional`이 없습니다.

### 🔵 Suggestion
1. **[가독성]** `buildResponse()` 헬퍼 메서드로 추출하면 가독성이 좋아집니다.

---
**총평**: Critical 1건, Warning 1건 발견. 레이어 위반 수정 후 머지 권장.
```

문제가 없으면:
```
**총평**: 아키텍처 규칙, 인증, 예외 처리, 응답 형식 모두 이상 없습니다. 머지 가능합니다.
```

---

## 빠른 리뷰 체크리스트

리뷰 시작 전 코드를 읽으며 아래를 빠르게 체크:

| # | 항목 | 확인 |
|---|------|------|
| 1 | Controller → Service → Repository 흐름만 존재 | |
| 2 | 인증 엔드포인트에 `@PreAuthorize` + `@AssignUserId` | |
| 3 | userId를 클라이언트에서 받지 않음 | |
| 4 | 모든 시간은 서버에서 생성 | |
| 5 | `BusinessException(ExceptionType.XXX)` 사용 | |
| 6 | `ResponseUtil.createSuccessResponse()` 사용 | |
| 7 | 쓰기에 `@Transactional`, 읽기에 `readOnly = true` | |
| 8 | Request DTO에 `@Valid` + 필드 검증 어노테이션 | |
| 9 | N+1 쿼리 없음 | |
| 10 | Native Query에 파라미터 바인딩(`:param`) 사용 | |
