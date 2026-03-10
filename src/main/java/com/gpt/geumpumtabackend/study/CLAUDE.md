# Study Domain CLAUDE.md

## 개요

학습 세션(타이머) 관리 도메인. 공부 시작/종료, 시간 계산, 오늘의 학습 시간 조회를 담당한다. 모든 타임스탬프는 서버에서 관리하며, 시작 시 캠퍼스 Wi-Fi 검증을 수행한다.

## 파일 구조

```
study/
├── api/
│   └── StudySessionApi.java              # Swagger 문서 인터페이스
├── controller/
│   └── StudySessionController.java       # /api/v1/study/*
├── domain/
│   ├── StudySession.java                 # 학습 세션 엔티티
│   └── StudyStatus.java                  # enum: STARTED(진행중), FINISHED(완료)
├── dto/
│   ├── request/
│   │   ├── StudyStartRequest.java        # record(gatewayIp, clientIp) — 클라이언트 타임스탬프 없음
│   │   └── StudyEndRequest.java          # record(studySessionId)
│   └── response/
│       ├── StudyStartResponse.java       # record(studySessionId)
│       └── StudySessionResponse.java     # record(totalStudySession, isStudying)
├── repository/
│   └── StudySessionRepository.java       # JPA + 통계/랭킹 Native Query
└── service/
    └── StudySessionService.java          # 핵심 비즈니스 로직
```

## 핵심 비즈니스 로직

### 학습 세션 생명주기

```
시작 요청 → Wi-Fi 검증 → 중복 세션 확인 → StudySession 생성 (STARTED, 서버 시간)
                                                    ↓
종료 요청 → 세션 조회 → endTime 설정 (서버 시간) → totalMillis 계산 → FINISHED

```

### 최대 공부시간
```
만약 사용자가 3시간 공부했다 -> 종료시켜야함

1. 서버에서 스케줄러를 통해 3시간 이상 진행중인 공부세션이 있는지 체크한다.
2. 만약 있다면, end_time을 공부시작 시간으로부터 3시간 이후로 설정한다.
3. FCM에 메세지를 보내서 클라이언트가 
```

### StudySession 엔티티
```java
// 주요 필드
Long id
LocalDateTime startTime       // 서버에서 설정
LocalDateTime endTime         // 서버에서 설정
Long totalMillis              // Duration.between(startTime, endTime).toMillis()
StudyStatus status            // STARTED → FINISHED
User user                     // @ManyToOne(LAZY)
```

- `startStudySession()`: startTime과 STARTED 상태 설정
- `endStudySession()`: endTime 설정, totalMillis 계산, FINISHED로 전환. endTime < startTime이면 예외

### StudySessionService 주요 메서드

| 메서드 | 설명 |
|--------|------|
| `startStudySession()` | Wi-Fi 검증 → 중복 STARTED 세션 확인 → 새 세션 생성 |
| `endStudySession()` | 세션 조회 → 서버 시간으로 종료 처리 |
| `getTodayStudySession()` | 오늘 00:00~현재 총 공부 시간 + 현재 진행 중 여부 |
| `verifyCampusWifiConnection()` | CampusWiFiValidationService 호출, 결과에 따라 예외 발생 |

### 서버 사이드 시간 관리
클라이언트 타임스탬프를 **절대 사용하지 않는다**. 모든 시간은 `LocalDateTime.now()`로 서버에서 생성.
- `StudyStartRequest`에 시간 필드 없음 (gatewayIp, clientIp만)
- `StudyEndRequest`에 시간 필드 없음 (studySessionId만)

## API 엔드포인트

모든 엔드포인트: `@AssignUserId` + `@PreAuthorize("isAuthenticated() and hasRole('USER')")`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/study` | 오늘의 학습 시간 조회 |
| POST | `/api/v1/study/start` | 학습 세션 시작 |
| POST | `/api/v1/study/end` | 학습 세션 종료 |

## Repository 쿼리

`StudySessionRepository`는 study 도메인 외에도 **랭킹, 통계 도메인에서 사용하는 Native Query**를 다수 포함:

| 쿼리 메서드 | 사용처 |
|---|---|
| `findByIdAndUser_Id()`, `findByUser_IdAndStatus()` | Study 도메인 |
| `sumCompletedStudySessionByUserId()` | Study 도메인 (오늘 총 시간) |
| `calculateCurrentPeriodRanking()` | Rank 도메인 (실시간 개인 랭킹) |
| `calculateCurrentPeriodDepartmentRanking()` | Rank 도메인 (실시간 학과별 개인 랭킹) |
| `calculateCurrentDepartmentRanking()` | Rank 도메인 (실시간 학과 랭킹) |
| `calculateFinalizedPeriodRanking()` | Rank 도메인 (확정 개인 랭킹) |
| `calculateFinalizedDepartmentRanking()` | Rank 도메인 (확정 학과 랭킹) |
| `getTwoHourSlotStats()`, `getDayMaxFocusAndFullTime()` | Statistics 도메인 |
| `getWeeklyStatistics()`, `getMonthlyStatistics()` | Statistics 도메인 |
| `getGrassStatistics()` | Statistics 도메인 (잔디 차트) |

실시간 랭킹 쿼리는 진행 중인 세션(`STARTED`)의 시간도 `LEAST/GREATEST`로 기간 겹침을 계산하여 포함한다.

## 테스트

### Unit Tests (`StudySessionServiceTest`)
- Wi-Fi 검증 성공/실패(INVALID/ERROR) 시 예외 처리
- 시간 계산: 정상(90분), 매우 짧은 세션(1초), 자정 넘김
- 초기 세션 상태 검증

### Integration Tests (`StudySessionControllerIntegrationTest`)
- 정상 시작/종료 플로우
- 인증 없이 요청 시 403, 잘못된 토큰 시 401
- 오늘의 공부 기록 조회, 빈 응답, 다른 사용자 데이터 격리
- 시작~종료 전체 플로우 E2E

## 개발 시 주의사항

1. **시간은 반드시 서버에서** — 클라이언트 시간 파라미터 추가 금지
2. **중복 세션 방지** — STARTED 상태 세션이 있으면 새 세션 생성 불가
3. `StudySessionRepository`의 Native Query 수정 시 랭킹/통계 도메인에 영향 — 반드시 관련 테스트 실행
4. Wi-Fi 검증은 `wifi` 도메인에 위임 — `CampusWiFiValidationService` 참조
