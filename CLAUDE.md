# CLAUDE.md

이 문서는 Claude Code가 본 코드베이스에서 작업할 때 참고하는 온보딩 가이드다.

---

## WHY — 프로젝트 존재 이유

**Geumpumta(열정품은타이머)** 는 금오공과대학교 학생들이 **캠퍼스 안에서** 공부 시간을 측정·경쟁하는 모바일 앱의 백엔드다.

핵심 가치:
- **장소 인증**: 캠퍼스 Wi-Fi에 접속한 상태에서만 공부 타이머 시작 가능 → 실제 등교를 보장
- **서버 기반 시간 관리**: 클라이언트 타임스탬프를 **절대 신뢰하지 않음** → 모든 시간은 서버에서 생성
- **경쟁·동기부여**: 개인/학과/시즌 랭킹으로 학습 동기 유발
- **대학 인증**: OAuth2 소셜 로그인 + @kumoh.ac.kr 이메일 인증으로 재학생만 이용

---

## WHAT — 시스템이 하는 일

### 도메인 요약

| 도메인 | 역할 | 핵심 엔티티 |
|--------|------|-------------|
| `study` | 학습 세션 시작/종료, 시간 계산 | `StudySession` |
| `rank` | 개인/학과/시즌 랭킹 계산·저장 | `UserRanking`, `DepartmentRanking`, `Season`, `SeasonRankingSnapshot` |
| `statistics` | 일간/주간/월간 통계, 잔디 차트 | (엔티티 없음, 쿼리 기반) |
| `user` | 사용자 관리, 이메일 인증, 프로필 | `User` |
| `token` | JWT 토큰 발급/갱신 | `RefreshToken` |
| `board` | 공지사항 게시판 | `Board` |
| `image` | Cloudinary 프로필 이미지 업로드 | (엔티티 없음) |
| `wifi` | 캠퍼스 Wi-Fi 네트워크 검증 | (엔티티 없음, 설정 기반) |
| `fcm` | Firebase 푸시 알림 | (토큰은 `User.fcmToken`에 저장) |

### 전체 API 엔드포인트

#### 학습 세션 (`/api/v1/study`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/api/v1/study` | 오늘의 총 공부 시간 + 진행중 여부 | USER |
| POST | `/api/v1/study/start` | 세션 시작 (Wi-Fi 검증 필수) | USER |
| POST | `/api/v1/study/end` | 세션 종료 | USER |

#### 개인 랭킹 (`/api/v1/rank/personal`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/daily?date=` | 일간 개인 랭킹 (date 없으면 실시간) | USER |
| GET | `/weekly?date=` | 주간 개인 랭킹 (월요일 기준) | USER |
| GET | `/monthly?date=` | 월간 개인 랭킹 (1일 기준) | USER |

#### 학과 랭킹 (`/api/v1/rank/department`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/daily?date=` | 일간 학과 랭킹 | USER |
| GET | `/weekly?date=` | 주간 학과 랭킹 | USER |
| GET | `/monthly?date=` | 월간 학과 랭킹 | USER |

#### 시즌 랭킹 (`/api/v1/rank/season`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/current` | 현재 시즌 전체 랭킹 | USER |
| GET | `/current/department?department=` | 현재 시즌 학과별 랭킹 | USER |
| GET | `/{seasonId}` | 종료된 시즌 전체 랭킹 (스냅샷) | USER |
| GET | `/{seasonId}/department?department=` | 종료된 시즌 학과별 랭킹 | USER |

#### 통계 (`/api/v1/statistics`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/day?date=&targetUserId=` | 일간 통계 (2시간 슬롯) | USER |
| GET | `/week?date=&targetUserId=` | 주간 통계 | USER |
| GET | `/month?date=&targetUserId=` | 월간 통계 | USER |
| GET | `/grass?date=&targetUserId=` | 잔디 차트 | USER |

#### 사용자 (`/api/v1/user`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/complete-registration` | 회원가입 완료 (학교 이메일, 학과, 학번) | GUEST |
| GET | `/profile` | 프로필 조회 | USER |
| GET | `/nickname/verify?nickname=` | 닉네임 중복 확인 | USER |
| POST | `/profile` | 프로필 수정 | USER |
| DELETE | `/logout` | 로그아웃 | USER |
| DELETE | `/withdraw` | 회원 탈퇴 (soft delete) | USER |
| POST | `/restore` | 탈퇴 복구 | USER |

#### 이메일 (`/api/v1/email`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/request-code` | 학교 이메일 인증 코드 발송 | GUEST |
| POST | `/verify-code` | 인증 코드 검증 | GUEST |

#### 토큰 (`/api/v1/token`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/refresh` | 액세스/리프레시 토큰 갱신 | 없음 |

#### 게시판 (`/api/v1/board`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/list` | 공지 목록 | USER |
| GET | `/{boardId}` | 공지 상세 | USER |
| POST | `/` | 공지 작성 | ADMIN |
| DELETE | `/{boardId}` | 공지 삭제 | ADMIN |

#### 이미지 (`/api/v1/image`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/profile` | 프로필 이미지 업로드 (최대 10MB) | USER |

#### FCM (`/api/v1/fcm`)
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/register` | FCM 디바이스 토큰 등록 | USER |
| DELETE | `/token` | FCM 토큰 삭제 | USER |

### 에러 코드 체계 (`ExceptionType`)

| 접두사 | 도메인 | 예시 |
|--------|--------|------|
| C | 공통 | `C001` UNEXPECTED_SERVER_ERROR, `C002` BINDING_ERROR |
| S | 보안 | `S001`~`S006` OAuth/JWT 관련 |
| T | 토큰 | `T001` REFRESH_TOKEN_NOT_EXIST, `T002` TOKEN_NOT_MATCHED |
| U | 사용자 | `U001`~`U006` 사용자/학교이메일/학번 관련 |
| M | 메일 | `M001` CANT_SEND_MAIL |
| ST | 학습 | `ST001`~`ST003` 세션 미발견/중복/시간 오류 |
| W | WiFi | `W001`~`W003` 네트워크 검증 |
| I | 이미지 | `I001`~`I003` 파일/크기/업로드 |
| B | 게시판 | `B001` BOARD_NOT_FOUND |
| SE | 시즌 | `SE001`~`SE005` 시즌 관련 |
| F | FCM | `F001`~`F003` 푸시 알림 |

### 스케줄러

| 작업 | Cron | 설명 |
|------|------|------|
| 일간 랭킹 확정 | `5 0 0 * * *` | 매일 00:00:05 — 전일 랭킹 계산·저장 |
| 주간 랭킹 확정 | `0 1 0 ? * MON` | 매주 월요일 00:01 |
| 월간 랭킹 확정 | `0 2 0 1 * ?` | 매월 1일 00:02 |
| 시즌 전환 확인 | `0 5 0 * * *` | 매일 00:05 — 시즌 종료 시 스냅샷 생성 + 다음 시즌 시작 |
| 최대 공부시간 체크 | `0 */10 * * * *` | 10분마다 — 3시간 초과 세션 자동 종료 + FCM 알림 |
| 만료 리프레시 토큰 삭제 | 별도 스케줄러 | 만료된 RefreshToken 정리 |

---

## HOW — 기술 구현

### 기술 스택

| 구분 | 기술 |
|------|------|
| 언어/프레임워크 | Java 21, Spring Boot 3.5.6, Gradle |
| 인증 | Spring Security + OAuth2 Client (Kakao, Google, Apple) |
| JWT | JJWT 0.12.6 + Nimbus JOSE JWT 9.37.4 (Apple용) |
| 데이터베이스 | Spring Data JPA + MySQL 8 |
| 캐시 | Spring Data Redis + Caffeine (로컬 캐시) |
| API 문서 | SpringDoc OpenAPI 2.8.14 |
| 이미지 | Cloudinary 1.39.0 |
| 네트워크 | Apache Commons Net 3.11.1 (CIDR 검증) |
| 재시도 | Spring Retry + Spring Aspects |
| 푸시 알림 | Firebase Admin SDK 9.7.1 |
| 모니터링 | Micrometer + Prometheus |
| 테스트 | JUnit 5, Mockito, TestContainers (MySQL 8.0, Redis 7.0) |
| 기타 | Lombok |

### 프로젝트 구조

```
src/main/java/com/gpt/geumpumtabackend/
├── GeumpumtaBackendApplication.java
│
├── global/                    # 공통 인프라
│   ├── aop/                   # @AssignUserId — JWT에서 userId 자동 주입
│   ├── base/                  # BaseEntity (createdAt, updatedAt, deletedAt)
│   ├── config/                # cache, fcm, image, mail, redis, retry, security, swagger
│   ├── exception/             # GlobalExceptionHandler, BusinessException, ExceptionType
│   ├── jwt/                   # JwtHandler, TokenProvider, JwtAuthenticationFilter
│   ├── oauth/                 # OAuth2 (Kakao/Google/Apple), handlers, resolvers
│   ├── response/              # ResponseUtil, ResponseBody, GlobalPageResponse
│   └── scheduler/             # RefreshTokenDeleteScheduler
│
├── board/                     # 게시판 (CRUD, soft delete)
├── fcm/                       # FCM 푸시 알림 (토큰 등록/삭제, 메시지 발송)
├── image/                     # 이미지 업로드 (Cloudinary)
├── rank/                      # 랭킹 시스템 (개인/학과/시즌, 스케줄러)
├── statistics/                # 통계 (일간/주간/월간, 잔디 차트)
├── study/                     # 학습 세션 (시작/종료, Wi-Fi 검증, 최대 시간 스케줄러)
├── token/                     # JWT 토큰 관리 (발급/갱신)
├── user/                      # 사용자 관리, 이메일 인증, 프로필
└── wifi/                      # 캠퍼스 Wi-Fi 검증 (CIDR, 캐싱)
```

각 도메인 모듈은 `api/ → controller/ → service/ → repository/ → domain/ → dto/` 계층 구조를 따른다.

### 아키텍처 패턴

#### Layered Architecture
`Controller → Service → Repository → Entity`. Controller는 HTTP 처리만 담당하고, Service에 비즈니스 로직을 집중한다.

#### AOP 사용자 컨텍스트 주입
```java
@PreAuthorize("isAuthenticated() and hasRole('USER')")
@AssignUserId  // JWT에서 userId 자동 주입
public ResponseEntity<T> endpoint(Long userId) { ... }
```

#### 표준 응답 형식
```java
ResponseUtil.createSuccessResponse(data);
ResponseUtil.createFailureResponse(ExceptionType.ERROR_TYPE);
```

#### 예외 처리
- `GlobalExceptionHandler`(`@RestControllerAdvice`)에서 전역 처리
- `ExceptionType` enum으로 에러 코드/메시지/HTTP 상태 관리
- 도메인 예외는 `BusinessException` 상속

#### Soft Delete
모든 엔티티가 `BaseEntity` 상속 → `createdAt`, `updatedAt`, `deletedAt` 자동 관리. `User`는 `@SQLDelete`로 삭제 시 데이터 마스킹 처리.

#### 이중 랭킹 구조
- **실시간 랭킹**: `StudySessionRepository` Native Query로 현재 기간 직접 계산 (진행중 세션 포함)
- **확정 랭킹**: 기간 종료 후 `UserRanking`/`DepartmentRanking` 테이블에 저장
- 컨트롤러에서 `date` 파라미터 유무로 분기

#### 시즌 시스템
4개 시즌 순환: `SPRING_SEMESTER`(3~6월) → `SUMMER_VACATION`(7~8월) → `FALL_SEMESTER`(9~12월) → `WINTER_VACATION`(1~2월). 시즌 종료 시 `SeasonRankingSnapshot`으로 불변 이력 저장 (Spring Retry 3회, 5초 backoff, JDBC 배치 2000건 청크).

#### 학과 랭킹 계산
학과별 상위 30명의 공부 시간 합산. Native Query + CTE로 25개 학과 처리. MySQL 8+ 필수.

### 인증 플로우

```
OAuth2 로그인 (Kakao/Google/Apple)
  → GUEST 역할로 User 생성
  → 학교 이메일 인증 (@kumoh.ac.kr)
  → 학과/학번 입력 → USER 역할로 승격
  → JWT 발급 (Access + Refresh, 14일)
  → API 요청 시 Authorization: Bearer <token> 헤더
  → JwtAuthenticationFilter → @PreAuthorize → @AssignUserId
```

역할 계층: `ADMIN` ⊃ `USER` ⊃ `GUEST`

### 프로파일 설정

| Profile | DB | DDL | 용도 |
|---------|-----|-----|------|
| `local` | MySQL localhost:3311 | create-drop | 로컬 개발 |
| `dev` | Docker MySQL | update | 개발 서버 |
| `prod` | Production DB | validate | 운영 |
| `test` | TestContainers MySQL 8.0 | - | 통합 테스트 |
| `unit-test` | H2 | - | 단위 테스트 |

### 민감 설정 (Git Submodule)
`src/main/resources/security/` 디렉토리에 git submodule로 관리. **절대 직접 커밋 금지.**
- `application-database.yml`, `application-security.yml`, `application-mail.yml`
- `application-swagger.yml`, `application-wifi.yml`, `application-cloudinary.yml`

### CI/CD

**GitHub Actions:**
- **CI** (dev/prod): Java 21 빌드, TestContainers + Redis 서비스 연동, 테스트, JUnit 리포트
- **CD** (dev): Docker 멀티 아키텍처 빌드 (AMD64/ARM64) → GHCR 푸시 → self-hosted 배포
- **CD** (prod): 아티팩트 기반 Docker 빌드 → GHCR → production 환경 배포

**Docker:**
- Base image: `amd64/openjdk:21-jdk-slim`
- `docker-compose.yml`: MySQL 8.4.0 (포트 3311) + Redis Alpine (포트 6379)

---

## Build & Run

```bash
# 인프라 실행 (MySQL 8.4, Redis)
docker-compose up -d

# 빌드
./gradlew clean build

# 로컬 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 전체 테스트
./gradlew test

# 단일 테스트 클래스
./gradlew test --tests "ClassName"
```

## 테스트

### 단위 테스트 (`src/test/java/.../unit/`)
- JUnit 5 + Mockito + AssertJ, `BaseUnitTest` 기반
- 프로파일: `unit-test` (H2, Redis 비활성)
- 대상: StudySession 시간 계산, 랭킹 로직, Wi-Fi 검증, 시즌 서비스, 시즌 스냅샷 재시도

### 통합 테스트 (`src/test/java/.../integration/`)
- TestContainers MySQL 8.0 + Redis 7.0, `BaseIntegrationTest` 기반
- 프로파일: `test`
- 각 테스트 후 전체 테이블 TRUNCATE + Redis FLUSHALL
- 대상: Controller E2E 테스트

---

## Development Checklist

1. 도메인 모듈 구조 준수: `api/ → controller/ → service/ → repository/ → domain/ → dto/`
2. 인증 필요 엔드포인트에 `@AssignUserId` + `@PreAuthorize` 사용
3. `@Transactional` 적절히 적용 (읽기 전용은 `readOnly = true`)
4. `ResponseUtil`로 응답 표준화
5. 새 에러는 `ExceptionType` enum에 추가 (접두사 규칙 준수)
6. `security/` 디렉토리 파일 커밋 금지
7. `StudySessionRepository` Native Query 수정 시 랭킹/통계 도메인 영향 확인
8. 시간은 반드시 서버에서 생성 — 클라이언트 타임스탬프 파라미터 추가 금지
9. 시즌/캐시 관련 코드 수정 시 `activeSeason` 캐시 eviction 로직 확인
