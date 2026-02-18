# ARCHITECTURE.md

Geumpumta 백엔드 시스템 아키텍처 문서.

---

## 1. 시스템 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                      클라이언트 (모바일 앱)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Security Filter Chain                                          │
│  CORS → OAuth2Login → JwtAuthenticationFilter → @PreAuthorize   │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer  (@AssignUserId AOP → userId 자동 주입)         │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                  │
│  study │ rank │ statistics │ user │ token │ board │ fcm │ wifi  │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer  (JPA │ Native Query │ JDBC Batch │ Redis)    │
├─────────────────────────────────────────────────────────────────┤
│  Scheduler Layer                                                │
│  RankingScheduler │ SeasonTransition │ MaxFocus │ TokenCleanup  │
└────────┬──────────┬──────────┬──────────┬───────────────────────┘
         │          │          │          │
   ┌─────▼───┐ ┌───▼────┐ ┌──▼───┐ ┌───▼──────┐
   │ MySQL 8 │ │ Redis  │ │ FCM  │ │Cloudinary│
   └─────────┘ └────────┘ └──────┘ └──────────┘
```

---

## 2. 엔티티 관계도

```
                  ┌─────────────┐
                  │    User     │
                  │ role        │ GUEST → USER → ADMIN
                  │ department  │ Enum (25개 학과)
                  │ provider    │ KAKAO, GOOGLE, APPLE
                  │ fcmToken    │
                  └──────┬──────┘
                         │
          ┌──────────────┼──────────────┐
          │ 1:N (FK)     │ 1:N (FK)     │ 1:N (FK 없음)
          ▼              ▼              ▼
   ┌─────────────┐ ┌───────────┐ ┌─────────────┐
   │StudySession │ │UserRanking│ │RefreshToken │
   │ startTime   │ │ rank      │ │ userId      │
   │ endTime     │ │ totalMillis│ │ refreshToken│
   │ totalMillis │ │ rankingType│ │ expiredAt   │
   │ status      │ │calculatedAt│ └─────────────┘
   └─────────────┘ └───────────┘

┌──────────────────┐  ┌───────────────────────┐  ┌────────┐
│DepartmentRanking │  │SeasonRankingSnapshot  │  │ Season │
│ department (Enum)│  │ seasonId (FK없음)      │  │ type   │
│ rank, totalMillis│  │ userId   (FK없음)      │  │ status │
│ rankingType      │  │ rankType, finalRank   │  │ start  │
│ calculatedAt     │  │ department (nullable) │  │ end    │
└──────────────────┘  └───────────────────────┘  └────────┘
```

**설계 결정:**
- `SeasonRankingSnapshot`에 FK 없음 → 시즌/유저 삭제 후에도 이력 보존
- `RefreshToken`에 FK 없음 → 유저 soft-delete와 독립적으로 토큰 정리
- `User` soft-delete 시 `deleted_` prefix → unique 제약 유지하면서 재가입 허용

---

## 3. 인증 플로우

```
[OAuth2 로그인]
앱 → /oauth2/authorization/{provider}?redirect_uri=...
  → CustomAuthorizationRequestResolver (redirect_uri를 state에 인코딩)
  → OAuth2 Provider 인증
  → CustomOAuth2UserService.loadUser() → User 조회/생성 (role=GUEST)
  → SuccessHandler → JWT 발급 → redirect_uri?accessToken=...&refreshToken=...

[회원가입 완료]
POST /email/request-code → Redis에 인증코드 (TTL 5분)
POST /email/verify-code  → 코드 검증
POST /user/complete-registration → GUEST→USER 승격, 새 JWT 발급

[API 요청]
Authorization: Bearer {token}
  → JwtAuthenticationFilter → parseToken (JJWT, HMAC-SHA256)
  → withdrawn=true이면 /restore 외 차단
  → @PreAuthorize → @AssignUserId AOP → Controller
```

---

## 4. 랭킹 시스템

### 이중 랭킹 구조

```
date 파라미터 유무로 분기:

date 없음 (현재 기간)              date 있음 (과거 기간)
  │                                │
  ▼                                ▼
실시간 랭킹                       확정 랭킹
StudySession Native Query로       UserRanking / DepartmentRanking
직접 계산 (진행중 세션 포함)         테이블에서 조회 (스케줄러가 저장)
```

### 시즌 랭킹 계산

```
현재 시즌 랭킹 = ① + ② + ③ 합산 후 순위 부여

① 확정 월간 합산 (시즌 시작 ~ 전월 말)
   → UserRankingRepository JPQL
② 현재 월 일간 합산 (이번 달 1일 ~ 어제)
   → UserRankingRepository JPQL
③ 오늘 실시간 데이터
   → StudySessionRepository Native Query

종료된 시즌 → SeasonRankingSnapshot 불변 스냅샷 조회 (계산 없음)
```

### 시즌 전환 (매일 00:05)

```
SeasonTransitionScheduler
  → 캐시 우회 DB 조회 → today ≥ endDate+1 ?
  → Yes: activeSeason 캐시 clear
       → transitionToNextSeason (현재=ENDED, 다음=ACTIVE)
       → createSeasonSnapshot (@Retryable 3회, JDBC 배치 2000건)
  → No: return
```

### 학과 랭킹

학과별 상위 30명의 공부 시간 합산. Native Query + CTE로 25개 학과 처리.
`ROW_NUMBER() PARTITION BY department` → 상위 30 필터 → `SUM GROUP BY` → `RANK()`.

---

## 5. 학습 세션 흐름

```
[시작] POST /study/start {gatewayIp, clientIp}
  → WiFi 검증 (@Cacheable) → 중복 STARTED 확인 → 세션 생성 (서버 시간)

[종료] POST /study/end {studySessionId}
  → 세션 조회 → endTime=서버시간, totalMillis=Duration 계산 → FINISHED

[자동종료] 매 10분 스케줄러
  → STARTED + 3시간 초과 세션 → 자동 종료 + FCM 알림
```

---

## 6. 크로스 도메인 의존성

### 서비스 의존 그래프

```
StudySessionService ──→ CampusWiFiValidationService, FcmService
PersonalRankService ──→ StudySessionRepository, UserRankingRepository
DepartmentRankService → StudySessionRepository, DepartmentRankingRepository
SeasonRankService ────→ SeasonService(@Cacheable), UserRankingRepo, StudySessionRepo
SeasonSnapshotService → UserRankingRepo, SeasonSnapshotBatchService(JDBC)
StatisticsService ────→ StudySessionRepository (12개 CTE)
UserService ──────────→ JwtHandler, RefreshTokenRepo, FcmService
TokenService ─────────→ JwtHandler, RefreshTokenRepo
```

### StudySessionRepository — 쿼리 허브

3개 도메인(study, rank, statistics)이 공유. 수정 시 전체 영향.

| 쿼리 | 도메인 | 용도 |
|------|--------|------|
| `calculateCurrentPeriodRanking` | rank | 실시간 개인 랭킹 |
| `calculateCurrentDepartmentRanking` | rank | 실시간 학과 랭킹 |
| `calculateFinalizedPeriodRanking` | rank | 확정 개인 랭킹 배치 |
| `calculateFinalizedDepartmentRanking` | rank | 확정 학과 랭킹 배치 |
| `getTwoHourSlotStats` | statistics | 일간 2시간 슬롯 |
| `getWeeklyStatistics` | statistics | 주간 통계 |
| `getMonthlyStatistics` | statistics | 월간 통계 |
| `getGrassStatistics` | statistics | 잔디 차트 (NTILE) |
| `sumCompletedStudySessionByUserId` | study | 오늘 총 공부 시간 |

---

## 7. 캐싱 전략

| 캐시 | 저장소 | 키 | TTL | 무효화 |
|------|--------|-----|-----|--------|
| `wifiValidation` | Caffeine | `gatewayIp:clientIp` | 10분 | 자동 만료 |
| `activeSeason` | Caffeine | 단일 엔트리 | 10분 | 시즌 전환 시 수동 clear |
| 이메일 인증코드 | Redis | `{userId}email:{email}` | 5분 | 자동 만료 |

---

## 8. 스케줄러 타임라인

```
매일:
00:00:00  RefreshTokenDelete     만료 토큰 삭제
00:00:05  DailyRanking           전일 개인/학과 랭킹 확정
00:05:00  SeasonTransition       시즌 종료 확인 → 전환/스냅샷
                                 ★ MonthlyRanking(00:02) 이후 실행 (데이터 의존)
월요일: 00:01  WeeklyRanking
1일:    00:02  MonthlyRanking
매 10분: MaxFocusStudy           3시간 초과 세션 자동 종료 + FCM
```

---

## 9. 예외 처리 경로

```
경로 1: Service 예외
  throw BusinessException(ExceptionType) → GlobalExceptionHandler
  → {"success":false, "code":"ST002", "msg":"..."}

경로 2: 인증 예외
  JwtAuthenticationFilter catch → HttpServletResponse 직접 JSON 작성
  → {"success":false, "code":"S004", "msg":"..."}

경로 3: Validation 예외
  @Valid MethodArgumentNotValidException → GlobalExceptionHandler
  → {"success":false, "code":"C002", "msg":"커스텀 메시지"}
```

```
예외 계층:
RuntimeException
  ├── BusinessException (ExceptionType: code + message + HttpStatus)
  └── JwtAuthenticationException
        ├── JwtTokenExpiredException   (S004, 401)
        ├── JwtTokenInvalidException   (S005, 401)
        ├── JwtNotExistException       (S006, 401)
        └── JwtAccessDeniedException   (S003, 403)

응답 구조:
ResponseBody<T> (sealed)
  ├── SuccessResponseBody<T> → {"success":true, "data":{...}}
  └── FailedResponseBody     → {"success":false, "code":"...", "msg":"..."}
```
