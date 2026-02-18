# AGENT.md

AI 에이전트(Claude Code 등)가 이 코드베이스에서 작업할 때 따라야 할 통합 가이드.

---

## 문서 계층 구조

```
CLAUDE.md                          ← 프로젝트 전체 온보딩 (WHY·WHAT·HOW)
ARCHITECTURE.md                    ← 시스템 아키텍처, 데이터 흐름
TESTING.md                         ← 테스트 전략, 커버리지 기준
AGENT.md (이 파일)                  ← AI 에이전트 행동 규칙

src/.../study/CLAUDE.md            ← 도메인별 상세 컨텍스트
src/.../rank/CLAUDE.md
src/.../wifi/CLAUDE.md

.claude/settings.json              ← 권한/보안 설정 (자동 적용)
```

**읽는 순서:** 에이전트는 작업 시작 전 `CLAUDE.md` → 작업 대상 도메인의 `CLAUDE.md` 순서로 컨텍스트를 로드한다.

---

## 에이전트 행동 규칙

### 1. 코드를 읽기 전에 수정하지 않는다

- 수정 대상 파일을 반드시 먼저 읽는다
- 기존 패턴을 파악한 후 동일한 스타일로 작성한다
- 추측으로 코드를 생성하지 않는다

### 2. 프로젝트 컨벤션을 따른다

| 규칙 | 내용 |
|------|------|
| 모듈 구조 | `api/ → controller/ → service/ → repository/ → domain/ → dto/` |
| 인증 | `@PreAuthorize("isAuthenticated() and hasRole('USER')")` + `@AssignUserId` |
| 응답 | `ResponseUtil.createSuccessResponse(data)` |
| 예외 | `throw new BusinessException(ExceptionType.XXX)` |
| DTO | `record` 사용, `@Valid` 바인딩 |
| Entity | `BaseEntity` 상속, `@Getter`, `@NoArgsConstructor(access = PROTECTED)` |
| 트랜잭션 | 클래스 `@Transactional(readOnly = true)` + 쓰기 메서드에 `@Transactional` |
| 시간 | 서버 `LocalDateTime.now()` — 클라이언트 타임스탬프 금지 |

### 3. 변경 영향 범위를 확인한다

| 변경 대상 | 영향 범위 | 확인 방법 |
|-----------|----------|----------|
| `StudySessionRepository` 쿼리 | study, rank, statistics 3개 도메인 | `rank/CLAUDE.md` 참고 |
| `SecurityConfig` | 전체 인증 체계 | 통합 테스트 전체 실행 |
| `ExceptionType` enum | 전체 예외 응답 | 접두사 규칙 확인 |
| `BaseEntity` | 전체 엔티티 | 모든 도메인 테스트 |
| `activeSeason` 캐시 관련 | 시즌 랭킹 전체 | `SeasonTransitionScheduler` 확인 |

### 4. 검증 후 제출한다

```bash
# 코드 수정 후 반드시 실행
./gradlew clean build              # 컴파일 확인

# 테스트 대상이 있으면
./gradlew test --tests "관련Test"   # 관련 테스트
./gradlew test                      # 전체 테스트 (권장)
```

---

## 보안 경계

### 접근 금지 영역

| 대상 | 이유 |
|------|------|
| `src/main/resources/security/` | git submodule 민감 설정 (DB 비밀번호, JWT 시크릿, OAuth 키) |
| `.env`, `*credential*`, `*secret*` | 자격증명 노출 방지 |

### 실행 금지 명령

| 명령 | 이유 |
|------|------|
| `git push`, `git push --force` | 원격 저장소 변경은 사람이 판단 |
| `git reset --hard`, `git clean -f` | 작업 내용 소실 위험 |
| `rm -rf` | 파일 대량 삭제 위험 |

### 수정 전 확인 필요 (Ask First)

| 대상 | 이유 |
|------|------|
| 기존 엔티티 필드 추가/변경 | DB 스키마 영향 |
| 스케줄러 cron 변경 | 랭킹/시즌 시스템 타이밍 영향 |
| `SecurityConfig` 변경 | 인증 체계 전체 영향 |
| Native Query 시그니처 변경 | 3개 도메인 영향 |

---

## 작업 유형별 가이드

### 새 도메인 추가

```
1. CLAUDE.md의 Module structure 확인
2. 패키지 생성: {domain}/api, controller, service, repository, domain, dto
3. Entity → Repository → Service → Controller → Api 순서로 구현
4. ExceptionType에 새 에러 코드 추가 (접두사 규칙)
5. 단위 테스트 작성 (Service)
6. 통합 테스트 작성 (Controller)
7. 빌드 확인: ./gradlew clean build
```

### 기존 기능 수정

```
1. 대상 파일 읽기
2. 도메인 CLAUDE.md 확인 (있으면)
3. 기존 테스트 확인 → 깨지는 테스트 없는지 파악
4. 수정
5. 영향받는 테스트 실행
6. 전체 빌드 확인
```

### 버그 수정

```
1. 재현 조건 파악
2. 관련 코드 읽기
3. 실패하는 테스트 먼저 작성 (가능하면)
4. 수정
5. 테스트 통과 확인
```

### 테스트 작성

```
1. TESTING.md의 커버리지 기준 확인
2. 대상 Service/Entity 읽기
3. BaseUnitTest 또는 BaseIntegrationTest 상속
4. given/when/then 패턴, @DisplayName 한글
5. 정상 + 예외 + 경계값
6. ./gradlew test --tests "ClassName" 실행 확인
```

---

## 도메인별 컨텍스트 파일

특정 도메인 작업 시 해당 `CLAUDE.md`를 먼저 읽는다.

| 도메인 | 컨텍스트 파일 | 핵심 내용 |
|--------|-------------|----------|
| study | `src/.../study/CLAUDE.md` | 세션 생명주기, 서버 시간 원칙, Repository 쿼리 공유 관계 |
| rank | `src/.../rank/CLAUDE.md` | 이중 랭킹 구조, 시즌 시스템, 스케줄러 cron, 배치 인서트 |
| wifi | `src/.../wifi/CLAUDE.md` | 검증 흐름, CIDR 설정, 캐시 키 구조 |
| 그 외 | 루트 `CLAUDE.md` | 프로젝트 전체 구조, 패턴, 체크리스트 |

---

## 프롬프트 패턴

에이전트에게 작업을 지시할 때 효과적인 패턴:

### 좋은 프롬프트

```
"StudySessionService에 getTodayStudySession 메서드의 단위 테스트를 작성해줘.
TESTING.md의 패턴을 따르고, 정상 케이스와 빈 데이터 케이스를 포함해."
```

```
"rank 도메인에 새 API를 추가해야 해.
rank/CLAUDE.md를 먼저 읽고, 기존 PersonalRankController 패턴을 따라 구현해줘."
```

### 나쁜 프롬프트

```
"랭킹 기능 만들어줘"              → 범위 불명확, 기존 코드 무시 가능
"모든 테스트 작성해줘"            → 범위 과대, 우선순위 없음
"이 코드 고쳐줘" (코드 미첨부)     → 대상 불명확
```

### 프롬프트 체크리스트

```
□ 대상 파일/메서드가 명시되어 있는가
□ 참고할 문서나 기존 패턴을 지정했는가
□ 기대 결과가 구체적인가
□ 범위가 한 작업 단위로 적절한가
```
