# Rank Domain

개인/학과/시즌 랭킹 계산·저장·조회를 담당하는 도메인.

---

## 1. 절대 규칙

- **`StudySessionRepository` Native Query 수정 시 랭킹/통계 도메인 영향 반드시 확인** — 실시간 랭킹이 이 쿼리에 직접 의존
- **`activeSeason` 캐시 eviction은 시즌 전환 전에 실행** — 순서 뒤바뀌면 stale 캐시로 잘못된 시즌 참조
- **스냅샷은 생성 후 절대 수정 금지** — `SeasonRankingSnapshot`은 불변 이력 레코드
- **`DepartmentRankingRepository` CTE 쿼리는 MySQL 8+ 전용** — H2에서 동작하지 않음
- **`SeasonSnapshotBatchService`는 JPA가 아닌 JDBC 직접 사용** — 트랜잭션 범위가 JPA와 분리됨

---

## 2. 아키텍처

### 파일 구조
```
rank/
├── api/                  # Swagger 문서 (PersonalRankApi, DepartmentRankApi, SeasonRankApi)
├── controller/           # PersonalRank, DepartmentRank, SeasonRank 컨트롤러
├── domain/
│   ├── UserRanking        # 확정 개인 랭킹 (rank, totalMillis, rankingType, calculatedAt)
│   ├── DepartmentRanking  # 확정 학과 랭킹 (department, rank, totalMillis, rankingType, calculatedAt)
│   ├── Season             # 시즌 (name, seasonType, startDate, endDate, status)
│   ├── SeasonRankingSnapshot  # 시즌 종료 시 불변 스냅샷 (seasonId, userId, rankType, finalRank, finalTotalMillis)
│   └── enums: RankType(OVERALL|DEPARTMENT), RankingType(DAILY|WEEKLY|MONTHLY),
│              SeasonType(4시즌), SeasonStatus(ACTIVE|ENDED)
├── dto/
│   ├── PersonalRankingTemp    # JPQL 프로젝션 DTO (Department enum/String 양쪽 생성자)
│   ├── DepartmentRankingTemp  # 학과 집계 DTO
│   └── response/              # PersonalRankingResponse, DepartmentRankingResponse,
│                                SeasonRankingResponse, SeasonDepartmentRankingResponse
├── repository/
│   ├── UserRankingRepository           # JPQL — 확정 랭킹 + 시즌 월간/일간 합산 쿼리
│   ├── DepartmentRankingRepository     # Native CTE — 25개 학과 랭킹 (MySQL 8+)
│   ├── SeasonRepository                # Native — 날짜 범위로 시즌 조회
│   └── SeasonRankingSnapshotRepository # 스냅샷 조회/존재 확인/학과 집계
├── service/
│   ├── PersonalRankService         # 개인 랭킹 조회 (실시간/확정)
│   ├── DepartmentRankService       # 학과 랭킹 조회
│   ├── SeasonRankService           # 시즌 랭킹 (월간+일간+실시간 병합, mergeAndRank)
│   ├── SeasonService               # 시즌 생명주기 (@Cacheable activeSeason)
│   ├── SeasonSnapshotService       # 스냅샷 생성 (@Retryable 3회, 5초 backoff)
│   └── SeasonSnapshotBatchService  # JDBC 배치 인서트 (2000건 청크)
└── scheduler/
    ├── RankingSchedulerService     # 일간/주간/월간 랭킹 확정
    └── SeasonTransitionScheduler   # 시즌 전환 + 스냅샷 생성
```

### 이중 랭킹 구조

| 구분 | 데이터 소스 | 트리거 |
|------|------------|--------|
| 실시간 | `StudySessionRepository` Native Query (진행중 세션 포함) | `date` 파라미터 없을 때 |
| 확정 | `UserRanking` / `DepartmentRanking` 테이블 | `date` 파라미터 있을 때 |

### 시즌 랭킹 계산 (3단 병합)
1. **확정 월간 합산** — seasonStart ~ (currentMonth - 1)의 월간 랭킹 SUM
2. **현재 월 일간 합산** — 1일 ~ (today - 1)의 일간 랭킹 SUM
3. **오늘 실시간** — `StudySessionRepository`에서 직접 계산
4. **병합** — userId/department별 GROUP BY → totalMillis SUM → RANK() (동점 처리)

### 동점 처리 (MySQL RANK 시맨틱)
```
Millis: 1000, 1000, 800 → Rank: 1, 1, 3  (2 건너뜀)
```

### 학과 랭킹
- 학과별 상위 30명 공부시간 합산 → 전체 학과 간 RANK
- CTE UNION ALL로 25개 학과 전부 포함 (0시간 학과도)
- 응답에서는 0시간 학과를 `topRanks`에서 제외하되, **본인 학과는 0이어도 항상 표시**

### Fallback
랭킹에 없는 사용자/학과: `rank = listSize + 1`, `totalMillis = 0`

---

## 3. 빌드 & 테스트

### 스케줄러 실행 순서 (겹침 방지)
| Cron | 작업 |
|------|------|
| `5 0 0 * * *` | 일간 랭킹 확정 (매일 00:00:05) |
| `0 1 0 ? * MON` | 주간 랭킹 확정 (월요일 00:01) |
| `0 2 0 1 * ?` | 월간 랭킹 확정 (매월 1일 00:02) |
| `0 5 0 * * *` | 시즌 전환 확인 (매일 00:05) |

### 단위 테스트 (`unit/rank/service/`)
- `PersonalRankServiceTest` — 실시간/확정 랭킹, fallback, 빈 리스트
- `DepartmentRankServiceTest` — 0시간 필터링, 본인 학과 포함, 학과명 변환
- `SeasonServiceTest` — 시즌 생성/전환, 4시즌 순환, 윤년 처리
- `SeasonSnapshotServiceRetryTest` — 재시도 3회, 중복 방지(idempotency)

### 통합 테스트 (`integration/rank/controller/`)
- `DepartmentRankControllerIntegrationTest` — E2E, 인증, 데이터 격리
- `SeasonRankControllerIntegrationTest` — 시즌 CRUD, 스냅샷 집계, 에러 케이스

---

## 4. 도메인 컨텍스트

### 시즌 시스템

| SeasonType | 기간 | 비고 |
|------------|------|------|
| `SPRING_SEMESTER` | 3/1 ~ 6/30 | |
| `SUMMER_VACATION` | 7/1 ~ 8/31 | |
| `FALL_SEMESTER` | 9/1 ~ 12/31 | |
| `WINTER_VACATION` | 1/1 ~ 2/28(29) | 윤년 처리 |

**생명주기**: ACTIVE 시즌 1개만 존재 → 스케줄러가 endDate+1 감지 → 캐시 evict → 스냅샷 생성(Retry 3회) → 현재 시즌 ENDED → 다음 시즌 ACTIVE

### 에러 코드
| 코드 | 이름 | 설명 |
|------|------|------|
| SE001 | SEASON_NOT_FOUND | 시즌 미발견 |
| SE002 | SEASON_NOT_ENDED | ACTIVE 시즌을 종료 시즌으로 조회 시도 |
| SE003 | SEASON_INVALID_DATE_RANGE | endDate ≤ startDate |
| SE004 | SEASON_ALREADY_ENDED | 이미 종료된 시즌 재종료 시도 |
| SE005 | NO_ACTIVE_SEASON | 활성 시즌 없음 |

### API 엔드포인트

**개인 랭킹** (`/api/v1/rank/personal`): `GET /daily`, `/weekly`, `/monthly` — 모두 `?date=` 선택적
**학과 랭킹** (`/api/v1/rank/department`): 동일 구조
**시즌 랭킹** (`/api/v1/rank/season`): `GET /current`, `/current/department?department=`, `/{seasonId}`, `/{seasonId}/department?department=`

모든 엔드포인트: `@PreAuthorize(USER)` + `@AssignUserId`

---

## 5. 코딩 컨벤션

1. `PersonalRankingTemp`에 Department enum/String 두 생성자 존재 — JPQL 프로젝션 방식에 따라 선택
2. 응답은 `ResponseUtil.createSuccessResponse(data)` 표준 형식
3. 읽기 전용 서비스 메서드에 `@Transactional(readOnly = true)`
4. 새 에러 코드는 `SE` 접두사 + `ExceptionType` enum에 추가
5. 랭킹 계산 로직 수정 시 동점 처리(RANK 시맨틱) 유지 확인
