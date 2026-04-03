# CLAUDE.md

**Geumpumta(열정품은타이머)** — 금오공과대학교 학생들이 캠퍼스 안에서 공부 시간을 측정·경쟁하는 모바일 앱 백엔드.

---

## 1. 절대 규칙

- **클라이언트 타임스탬프 절대 신뢰 금지** — 모든 시간은 서버에서 생성. 클라이언트 시간 파라미터 추가 금지
- **`security/` 디렉토리 파일 커밋 금지** — git submodule로 관리되는 민감 설정
- **`StudySessionRepository` Native Query 수정 시 랭킹/통계 도메인 영향 반드시 확인**
- **시즌/캐시 관련 코드 수정 시 `activeSeason` 캐시 eviction 로직 확인**

---

## 2. 아키텍처

### 기술 스택
Java 21 · Spring Boot 3.5.6 · Gradle · Spring Security + OAuth2 (Kakao/Google/Apple) · JPA + MySQL 8 · Redis + Caffeine · TestContainers · Firebase Admin SDK · Cloudinary · Spring Retry · Lombok

### 프로젝트 구조
```
src/main/java/com/gpt/geumpumtabackend/
├── global/           # 공통: AOP(@AssignUserId), BaseEntity, config, exception, jwt, oauth, response, scheduler
├── board/            # 게시판 CRUD
├── fcm/              # FCM 푸시 알림
├── image/            # Cloudinary 이미지 업로드
├── rank/             # 개인/학과/시즌 랭킹
├── statistics/       # 일간/주간/월간 통계, 잔디 차트
├── study/            # 학습 세션 (시작/종료, Wi-Fi 검증)
├── token/            # JWT 토큰 관리
├── user/             # 사용자 관리, 이메일 인증
└── wifi/             # 캠퍼스 Wi-Fi CIDR 검증
```

각 도메인: `api/ → controller/ → service/ → repository/ → domain/ → dto/`

### 핵심 패턴

**Layered Architecture**: Controller(HTTP만) → Service(비즈니스 로직) → Repository → Entity

**AOP 사용자 주입**:
```java
@PreAuthorize("isAuthenticated() and hasRole('USER')")
@AssignUserId  // JWT에서 userId 자동 주입
public ResponseEntity<T> endpoint(Long userId) { ... }
```

**표준 응답**: `ResponseUtil.createSuccessResponse(data)` / `ResponseUtil.createFailureResponse(ExceptionType.XXX)`

**예외 처리**: `GlobalExceptionHandler`(@RestControllerAdvice) + `ExceptionType` enum(코드/메시지/HTTP상태) + `BusinessException` 상속

**Soft Delete**: `BaseEntity`(createdAt, updatedAt, deletedAt) 상속. User는 `@SQLDelete`로 마스킹

**이중 랭킹**:
- 실시간: `StudySessionRepository` Native Query로 직접 계산 (진행중 세션 포함)
- 확정: 기간 종료 후 `UserRanking`/`DepartmentRanking` 테이블 저장
- `date` 파라미터 유무로 분기

**시즌**: 4개 순환 (`SPRING_SEMESTER` 3~6 → `SUMMER_VACATION` 7~8 → `FALL_SEMESTER` 9~12 → `WINTER_VACATION` 1~2). 종료 시 `SeasonRankingSnapshot` 불변 저장 (Retry 3회, JDBC 배치 2000건)

**학과 랭킹**: 상위 30명 합산. Native Query + CTE. MySQL 8+ 필수

**인증 플로우**: OAuth2 → GUEST 생성 → @kumoh.ac.kr 이메일 인증 → 학과/학번 → USER 승격 → JWT(Access+Refresh, 14일)
역할 계층: `ADMIN` ⊃ `USER` ⊃ `GUEST`

### 에러 코드 접두사
`C`(공통) · `S`(보안) · `T`(토큰) · `U`(사용자) · `M`(메일) · `ST`(학습) · `W`(WiFi) · `I`(이미지) · `B`(게시판) · `SE`(시즌) · `F`(FCM)

### 스케줄러
- 일간 랭킹 `5 0 0 * * *` / 주간 `0 1 0 ? * MON` / 월간 `0 2 0 1 * ?`
- 시즌 전환 `0 5 0 * * *` — 종료 시 스냅샷 + 다음 시즌 시작
- 최대 공부시간 `0 */10 * * * *` — 3시간 초과 세션 자동 종료 + FCM

---

## 3. 빌드 & 테스트

```bash
docker-compose up -d                                    # MySQL 8.4(3311) + Redis(6379)
./gradlew clean build                                   # 빌드
./gradlew bootRun --args='--spring.profiles.active=local' # 로컬 실행
./gradlew test                                          # 전체 테스트
./gradlew test --tests "ClassName"                      # 단일 클래스
```

| Profile | DB | DDL | 용도 |
|---------|-----|-----|------|
| `local` | MySQL localhost:3311 | create-drop | 로컬 개발 |
| `dev` | Docker MySQL | update | 개발 서버 |
| `prod` | Production DB | validate | 운영 |
| `test` | TestContainers MySQL 8.0 | - | 통합 테스트 |
| `unit-test` | H2 | - | 단위 테스트 |

**단위 테스트** (`src/test/.../unit/`): JUnit 5 + Mockito + AssertJ, `BaseUnitTest`, 프로파일 `unit-test`
**통합 테스트** (`src/test/.../integration/`): TestContainers, `BaseIntegrationTest`, 프로파일 `test`, 테스트 후 TRUNCATE + FLUSHALL

---

## 4. 도메인 컨텍스트

| 도메인 | 역할 | 핵심 엔티티 |
|--------|------|-------------|
| `study` | 학습 세션 시작/종료, 시간 계산 | `StudySession` |
| `rank` | 개인/학과/시즌 랭킹 | `UserRanking`, `DepartmentRanking`, `Season`, `SeasonRankingSnapshot` |
| `statistics` | 일/주/월 통계, 잔디 차트 | 쿼리 기반 |
| `user` | 사용자, 이메일 인증, 프로필 | `User` |
| `token` | JWT 발급/갱신 | `RefreshToken` |
| `board` | 공지사항 | `Board` |
| `wifi` | 캠퍼스 Wi-Fi 검증 | 설정 기반 |
| `fcm` | 푸시 알림 | `User.fcmToken` |

핵심 가치: **장소 인증**(캠퍼스 Wi-Fi 필수) · **서버 시간 관리** · **경쟁/동기부여**(랭킹) · **대학 인증**(@kumoh.ac.kr)

---

## 5. 코딩 컨벤션

1. 도메인 모듈 구조 준수: `api/ → controller/ → service/ → repository/ → domain/ → dto/`
2. 인증 엔드포인트에 `@AssignUserId` + `@PreAuthorize` 사용
3. `@Transactional` 적절히 적용 (읽기 전용은 `readOnly = true`)
4. `ResponseUtil`로 응답 표준화
5. 새 에러는 `ExceptionType` enum에 추가 (접두사 규칙 준수)

### 민감 설정 (Git Submodule)
`src/main/resources/security/`: `application-{database,security,mail,swagger,wifi,cloudinary}.yml`
