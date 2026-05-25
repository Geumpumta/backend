# AGENTS.md

이 레포에서 Codex는 코드를 바로 작성하지 않고, `docs/requirements` 아래의 도메인별 요구사항 문서를 기준으로 요구사항 기반 개발을 수행한다.

## 1. 문서 확인 순서

작업 시작 전 아래 순서로 필요한 문서를 확인한다.

1. `docs/requirements/README.md`
2. 작업 대상 도메인의 `docs/requirements/{domain}.md`
3. 공통 규칙이 필요한 경우 `docs/requirements/common.md`
4. 프로젝트 전체 컨텍스트가 필요한 경우 `CLAUDE.md`
5. 테스트 작업이면 `TESTING.md`
6. 아키텍처 변경이면 `ARCHITECTURE.md`
7. 도메인별 상세 컨텍스트가 있으면 해당 `src/.../{domain}/CLAUDE.md`

## 2. 요구사항 우선 원칙

- 새로운 기능을 구현하거나 기존 기능을 의미 있게 수정할 때는 반드시 먼저 요구사항 문서를 확인한다.
- 구현은 `docs/requirements/{domain}.md`에 정의된 요구사항을 기준으로 진행한다.
- 공통 응답, 예외, 권한, 시간 기준은 `docs/requirements/common.md`를 함께 확인한다.
- `docs/requirements`에 없는 동작을 임의로 추가하지 않는다.
- `docs/requirements`의 요구사항과 충돌하는 구현을 하지 않는다.
- 요구사항이 불명확하거나 변경이 필요하다고 판단되면, 코드를 먼저 수정하지 말고 요구사항 수정안 또는 개발 명세 수정안을 먼저 제안한다.

## 3. 요구사항 문서 위치

- 요구사항 인덱스: `docs/requirements/README.md`
- 공통 요구사항: `docs/requirements/common.md`
- 도메인 요구사항: `docs/requirements/auth.md`, `user.md`, `study.md`, `statistics.md`, `rank.md`, `board.md`, `badge.md`, `fcm.md`, `image.md`, `maintenance.md`
- 여러 도메인에 걸친 작업은 관련 도메인 문서와 `common.md`를 모두 확인한다.
- 새 도메인을 추가할 때는 구현 전에 `docs/requirements/{new-domain}.md`를 먼저 작성하고, `docs/requirements/README.md` 문서 목록에 추가한다.

## 4. 개발 순서

1. 요구사항 확인
2. 아키텍처 및 데이터 흐름 정리
3. 가능한 기술 선택지 비교
4. 개발 명세 작성 또는 수정
5. 구현
6. 요구사항 충족 여부 검증

## 5. 코드 작성 규칙

- 수정 대상 파일을 반드시 먼저 읽는다.
- 기존 패턴을 파악한 후 동일한 스타일로 작성한다.
- 추측으로 코드를 생성하지 않는다.
- 변경 영향 범위를 확인하고, 관련 도메인 요구사항과 테스트를 함께 확인한다.

## 6. 프로젝트 컨벤션

| 규칙 | 내용 |
|------|------|
| 모듈 구조 | `api/ -> controller/ -> service/ -> repository/ -> domain/ -> dto/` |
| 인증 | `@PreAuthorize("isAuthenticated() and hasRole('USER')")` + `@AssignUserId` |
| 응답 | `ResponseUtil.createSuccessResponse(data)` |
| 예외 | `throw new BusinessException(ExceptionType.XXX)` |
| DTO | `record` 사용, `@Valid` 바인딩 |
| Entity | `BaseEntity` 상속, `@Getter`, `@NoArgsConstructor(access = PROTECTED)` |
| 트랜잭션 | 클래스 `@Transactional(readOnly = true)` + 쓰기 메서드에 `@Transactional` |
| 시간 | 서버 `LocalDateTime.now()` 기준, 클라이언트 타임스탬프 신뢰 금지 |

## 7. 변경 영향 범위

| 변경 대상 | 영향 범위 | 확인 방법 |
|-----------|----------|----------|
| `StudySessionRepository` 쿼리 | study, rank, statistics | `study.md`, `rank.md`, `statistics.md` 확인 |
| `SecurityConfig` | 전체 인증 체계 | `auth.md`, `common.md`, 통합 테스트 확인 |
| `ExceptionType` enum | 전체 예외 응답 | `common.md`, 에러 코드 접두사 규칙 확인 |
| `BaseEntity` | 전체 엔티티 | 모든 도메인 테스트 확인 |
| `activeSeason` 캐시 관련 | 시즌 랭킹 전체 | `rank.md`, `SeasonTransitionScheduler` 확인 |

## 8. 요구사항 작성 기준

- 요구사항은 기술 선택이 아니라 사용자가 기대하는 동작이나 시스템이 만족해야 하는 조건이다.
- 좋은 요구사항: "앱은 1초 안에 CCTV 스트림 4개를 화면에 표시해야 한다."
- 나쁜 요구사항: "WebSocket을 사용한다."
- 각 요구사항 문서는 `목적`, `기능 요구사항`, `예외/제약사항`, `관련 API`, `검증 시나리오` 구성을 기본으로 한다.

## 9. 기술 선택 원칙

- 중요한 기술 선택을 할 때는 선택한 방식의 장점만 설명하지 않는다.
- 가능한 대안들을 먼저 나열한다.
- 각 대안의 장점, 단점, 리스크를 비교한다.
- 우리 프로젝트의 요구사항과 특성에 비추어 왜 특정 방식을 선택했는지 설명한다.
- 선택한 방식의 단점과 리스크에 대한 대응 방안도 함께 정리한다.

## 10. 깊이 있게 검토하는 방식

- 기능을 단순 구현 단위로만 보지 않는다.
- 하나의 기능에 대해 요구사항, 구현 방식 후보, 장단점 비교, 선택 근거, 리스크 대응까지 3~4단계 이상 깊게 검토한다.
- 예: 알림 기능 검토
  - 알림은 실시간이어야 하는가?
  - 실시간 알림 구현 방식에는 Polling, SSE, WebSocket, Push Notification 등이 있다.
  - 각 방식의 장점, 단점, 리스크를 비교한다.
  - 우리 서비스의 특성상 어떤 방식이 적합한지 판단한다.
  - 선택한 방식의 리스크와 보완책을 정리한다.

## 11. 선택한 기술의 동작 원리 이해

- 기술을 선택했다면 단순 사용법만 확인하지 않는다.
- 선택한 기술이 내부적으로 어떤 원리, 알고리즘, 프로토콜, 아키텍처 패턴으로 동작하는지 간단히 확인한다.
- 예: WebSocket, SSE, Polling은 연결 방식과 서버 자원 사용 방식이 다르다.
- 예: `synchronized`, `Atomic`은 동시성 제어 원리와 보장 범위가 다르다.

## 12. 문서화 규칙

- 새로운 기능, 아키텍처 변경, 중요한 기술 선택이 필요한 작업에서는 구현 전에 문서를 먼저 작성하거나 수정한다.
- 필요한 경우 `docs/requirements/{domain}.md`, `spec.md`, ADR 문서를 작성하거나 수정한다.
- 단순 오타 수정이나 작은 버그 수정처럼 영향 범위가 작은 작업에는 과한 문서화를 하지 않는다.

## 13. 보안 경계

### 접근 금지 영역

| 대상 | 이유 |
|------|------|
| `src/main/resources/security/` | git submodule 민감 설정 |
| `.env`, `*credential*`, `*secret*` | 자격증명 노출 방지 |

### 실행 금지 명령

| 명령 | 이유 |
|------|------|
| `git push`, `git push --force` | 원격 저장소 변경은 사람이 판단 |
| `git reset --hard`, `git clean -f` | 작업 내용 소실 위험 |
| `rm -rf` | 파일 대량 삭제 위험 |

### 수정 전 확인 필요

| 대상 | 이유 |
|------|------|
| 기존 엔티티 필드 추가/변경 | DB 스키마 영향 |
| 스케줄러 cron 변경 | 랭킹/시즌 시스템 타이밍 영향 |
| `SecurityConfig` 변경 | 인증 체계 전체 영향 |
| Native Query 시그니처 변경 | 여러 도메인 영향 |

## 14. 작업 유형별 가이드

### 새 도메인 추가

1. `docs/requirements/{new-domain}.md` 작성
2. `docs/requirements/README.md` 문서 목록 갱신
3. `CLAUDE.md`의 모듈 구조 확인
4. 패키지 생성: `{domain}/api`, `controller`, `service`, `repository`, `domain`, `dto`
5. Entity -> Repository -> Service -> Controller -> Api 순서로 구현
6. `ExceptionType`에 새 에러 코드 추가
7. 단위 테스트와 통합 테스트 작성
8. `./gradlew clean build` 실행

### 기존 기능 수정

1. 관련 요구사항 문서 확인
2. 대상 파일 읽기
3. 도메인 `CLAUDE.md` 확인
4. 기존 테스트 확인
5. 수정
6. 영향받는 테스트 실행
7. 요구사항 충족 여부 확인

### 버그 수정

1. 재현 조건 파악
2. 관련 요구사항 문서 확인
3. 관련 코드 읽기
4. 실패하는 테스트 작성
5. 수정
6. 테스트 통과 확인

### 테스트 작성

1. `TESTING.md`의 커버리지 기준 확인
2. 대상 Service 또는 Entity 읽기
3. `BaseUnitTest` 또는 `BaseIntegrationTest` 상속
4. given/when/then 패턴과 한글 `@DisplayName` 사용
5. 정상, 예외, 경계값 검증
6. `./gradlew test --tests "ClassName"` 실행

## 15. 도메인별 컨텍스트

| 도메인 | 컨텍스트 파일 | 핵심 내용 |
|--------|-------------|----------|
| study | `src/.../study/CLAUDE.md` | 세션 생명주기, 서버 시간 원칙, Repository 쿼리 공유 관계 |
| rank | `src/.../rank/CLAUDE.md` | 이중 랭킹 구조, 시즌 시스템, 스케줄러 cron, 배치 인서트 |
| wifi | `src/.../wifi/CLAUDE.md` | 검증 흐름, CIDR 설정, 캐시 키 구조 |
| 그 외 | 루트 `CLAUDE.md` | 프로젝트 전체 구조, 패턴, 체크리스트 |

## 16. 완료 기준

- 구현 완료 후 변경 내용이 `docs/requirements/{domain}.md`의 어떤 요구사항을 만족하는지 확인한다.
- 가능한 경우 테스트 또는 검증 방법을 함께 제시한다.
- 코드 수정 후 가능한 범위에서 관련 테스트를 실행한다.
- 영향 범위가 크면 `./gradlew test` 또는 `./gradlew clean build`를 실행한다.
