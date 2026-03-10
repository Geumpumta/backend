# Rank Domain CLAUDE.md

## 개요

개인/학과 랭킹 및 시즌 시스템을 담당하는 도메인. 실시간 랭킹 계산, 확정 랭킹 저장, 시즌 전환, 스냅샷 생성을 포함한다.

## 파일 구조

```
rank/
├── api/
│   ├── PersonalRankApi.java              # 개인 랭킹 Swagger 문서
│   ├── DepartmentRankApi.java            # 학과 랭킹 Swagger 문서
│   └── SeasonRankApi.java                # 시즌 랭킹 Swagger 문서
├── controller/
│   ├── PersonalRankController.java       # /api/v1/rank/personal/*
│   ├── DepartmentRankController.java     # /api/v1/rank/department/*
│   └── SeasonRankController.java         # /api/v1/rank/season/*
├── domain/
│   ├── UserRanking.java                  # 개인 랭킹 엔티티
│   ├── DepartmentRanking.java            # 학과 랭킹 엔티티
│   ├── Season.java                       # 시즌 엔티티 (기간 검증 포함)
│   ├── SeasonRankingSnapshot.java        # 시즌 종료 시 확정 랭킹 스냅샷
│   ├── RankType.java                     # enum: OVERALL, DEPARTMENT
│   ├── RankingType.java                  # enum: DAILY, WEEKLY, MONTHLY
│   ├── SeasonType.java                   # enum: SPRING_SEMESTER, SUMMER_VACATION, FALL_SEMESTER, WINTER_VACATION
│   └── SeasonStatus.java                 # enum: ACTIVE, ENDED
├── dto/
│   ├── PersonalRankingTemp.java          # JPQL 프로젝션용 DTO (userId, nickname, department, totalMillis, ranking)
│   ├── DepartmentRankingTemp.java        # 학과 집계용 DTO
│   └── response/
│       ├── PersonalRankingResponse.java      # topRanks + myRanking
│       ├── PersonalRankingEntryResponse.java # 개인 랭킹 항목
│       ├── DepartmentRankingResponse.java    # topRanks + myDepartmentRanking
│       ├── DepartmentRankingEntryResponse.java # 학과 랭킹 항목
│       └── SeasonRankingResponse.java        # 시즌 랭킹 (seasonId, seasonName, dates, rankings)
├── repository/
│   ├── UserRankingRepository.java            # 개인 랭킹 JPQL 쿼리
│   ├── DepartmentRankingRepository.java      # 학과 랭킹 Native Query (CTE 사용)
│   ├── SeasonRepository.java                 # 시즌 조회 (날짜 범위)
│   └── SeasonRankingSnapshotRepository.java  # 스냅샷 조회/존재 확인
├── service/
│   ├── PersonalRankService.java              # 개인 랭킹 조회 (실시간/확정)
│   ├── DepartmentRankService.java            # 학과 랭킹 조회
│   ├── SeasonRankService.java                # 시즌 랭킹 계산 (월간+일간+실시간 병합)
│   ├── SeasonService.java                    # 시즌 생명주기 관리 (@Cacheable)
│   ├── SeasonSnapshotService.java            # 스냅샷 생성 (@Retryable, 3회, 5초 backoff)
│   └── SeasonSnapshotBatchService.java       # JDBC 배치 인서트 (2000건 청크)
└── scheduler/
    ├── RankingSchedulerService.java          # 일간/주간/월간 랭킹 스케줄러
    └── SeasonTransitionScheduler.java        # 시즌 전환 스케줄러
```

## 핵심 개념

### 이중 랭킹 구조
- **실시간 랭킹**: `StudySessionRepository`에서 직접 계산 (현재 기간)
- **확정 랭킹**: 기간 종료 후 `UserRanking`/`DepartmentRanking`에 저장 (과거 기간)

컨트롤러에서 `date` 파라미터 유무로 분기:
- `date` 없음 → 현재 기간 실시간 랭킹
- `date` 있음 → 해당 날짜의 확정 랭킹

### 시즌 시스템
4개 시즌이 순환:
| SeasonType | 기간 |
|---|---|
| SPRING_SEMESTER | 3/1 ~ 6/30 |
| SUMMER_VACATION | 7/1 ~ 8/31 |
| FALL_SEMESTER | 9/1 ~ 12/31 |
| WINTER_VACATION | 1/1 ~ 2/28(29) |

시즌 랭킹 = 확정 월간 합산 + 현재 월 일간 합산 + 오늘 실시간 데이터를 `mergeAndRank()`로 병합.

### 학과 랭킹 계산
- 학과별 상위 30명의 공부 시간을 합산
- Native Query + CTE로 25개 학과 처리
- 공부 시간 0인 학과는 topRanks에서 제외하되, 본인 학과는 0이어도 표시

### Fallback 로직
랭킹에 포함되지 않은 사용자: `rank = listSize + 1`, `totalMillis = 0`

## 스케줄러 실행 시점

| 작업 | Cron | 설명 |
|------|------|------|
| 일간 랭킹 계산 | `5 0 0 * * *` | 매일 00:00:05 |
| 주간 랭킹 계산 | `0 1 0 ? * MON` | 매주 월요일 00:01 |
| 월간 랭킹 계산 | `0 2 0 1 * ?` | 매월 1일 00:02 |
| 시즌 전환 확인 | `0 5 0 * * *` | 매일 00:05 |

## API 엔드포인트

### 개인 랭킹 (`/api/v1/rank/personal`)
- `GET /daily?date=` — 일간 개인 랭킹
- `GET /weekly?date=` — 주간 개인 랭킹 (월요일 기준)
- `GET /monthly?date=` — 월간 개인 랭킹 (1일 기준)

### 학과 랭킹 (`/api/v1/rank/department`)
- `GET /daily?date=` — 일간 학과 랭킹
- `GET /weekly?date=` — 주간 학과 랭킹
- `GET /monthly?date=` — 월간 학과 랭킹

### 시즌 랭킹 (`/api/v1/rank/season`)
- `GET /current` — 현재 시즌 전체 랭킹
- `GET /current/department?department=` — 현재 시즌 학과별 랭킹
- `GET /{seasonId}` — 종료된 시즌 전체 랭킹
- `GET /{seasonId}/department?department=` — 종료된 시즌 학과별 랭킹

## 테스트

### Unit Tests
- `PersonalRankServiceTest` — 실시간/확정 랭킹, fallback, 동점 처리, 빈 리스트
- `DepartmentRankServiceTest` — 0시간 학과 필터링, 본인 학과 포함, 학과명 변환
- `SeasonRankServiceTest` — 데이터 병합, 동점 처리, 스냅샷 조회, 예외(SEASON_NOT_FOUND, SEASON_NOT_ENDED)
- `SeasonServiceTest` — 시즌 생성/전환, 4개 시즌 순환, 윤년 처리, 날짜 검증
- `SeasonSnapshotServiceRetryTest` — 재시도 메커니즘, 중복 방지

### Integration Tests
- `DepartmentRankControllerIntegrationTest` — E2E API 테스트, 인증, 데이터 격리

## 개발 시 주의사항

1. 랭킹 쿼리가 복잡하므로 `StudySessionRepository`의 Native Query도 함께 확인할 것
2. 시즌 전환 시 `activeSeason` 캐시가 evict됨 — 캐시 관련 코드 수정 시 주의
3. `SeasonSnapshotBatchService`는 JDBC 직접 사용 — JPA와 별도 트랜잭션
4. `DepartmentRankingRepository`의 Native Query는 CTE 사용 — MySQL 8+ 필수
5. `PersonalRankingTemp`에 Department enum/String 두 가지 생성자 존재 — JPQL 프로젝션 방식에 따라 다름
