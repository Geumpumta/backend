# Phase별 판단 기준 및 엣지케이스

막히거나 판단이 애매할 때 이 파일을 참고한다.

---

## Phase 1 — 장애 유형 분류 기준

### 심각도 판단
| 심각도 | 기준 |
|--------|------|
| Critical | prod 인스턴스 다운 / 5xx 에러율 > 10% / 전면 서비스 불가 |
| High | 특정 기능 불가 / 5xx 에러율 3~10% / 응답시간 p99 > 5초 |
| Medium | 일부 기능 저하 / 에러율 < 3% / 응답시간 p99 3~5초 |

### Grafana Alert 상태 해석
- `Alerting (NoData)`: 쿼리 결과가 없음 → 쿼리 레이블 오류이거나 스크레이핑 중단
- `Alerting`: 실제 조건 충족 → 진짜 장애
- `Pending`: 조건 충족 중이나 `for` 기간 미달 → 주시 필요

### Loki 로그가 없을 때
- Loki datasource 쿼리 실패 시 → "Loki 로그 조회 불가, Prometheus 메트릭만으로 분석" 명시
- 로그 없이 메트릭만으로 추정 원인 도출

---

## Phase 2 — 코드 탐색 전략

### 에러 코드가 없을 때
Grafana 로그에서 에러 코드가 식별되지 않으면:
1. 에러 발생 URI를 기준으로 컨트롤러 매핑 → 도메인 특정
2. 스택 트레이스에서 패키지명으로 도메인 특정

### 캐시 관련 장애 판단
다음 증상이면 Caffeine 캐시 문제를 우선 의심:
- 시즌 전환 직후 에러 급증
- `activeSeason` 관련 NullPointerException
- `SE` 접두사 에러 코드

→ `rank/service/SeasonService.java` + 캐시 eviction 로직 반드시 확인

### StudySessionRepository Native Query 수정 여부 확인
`study/` 도메인 장애 시:
- Native Query 변경이 랭킹/통계(`rank/`, `statistics/`)에 영향을 줄 수 있음
- 수정 전에 두 도메인 동시 검토 필수

---

## Phase 3 — Jira 티켓 생성 엣지케이스

### Atlassian MCP 연결 실패 시
Jira 티켓 생성이 불가하면:
1. 티켓 번호를 임시로 `INC-{YYYYMMDD-HHMM}` 형식으로 생성
2. 이후 Phase에서 이 임시 번호 사용
3. Phase 완료 후 "Jira에 수동으로 티켓 생성 필요" 안내

### 프로젝트 키 확인
Atlassian MCP로 Jira 프로젝트 목록 조회 후 가장 적합한 프로젝트 선택.
프로젝트가 없으면 사용자에게 프로젝트 키 확인 요청.

---

## Phase 4 — Worktree 생성 엣지케이스

### Worktree 경로 충돌 시
`../hotfix-{티켓번호}` 경로가 이미 존재하면:
```bash
git worktree list  # 기존 worktree 확인
git worktree remove ../hotfix-{이전티켓}  # 불필요한 worktree 정리
```

### dev 브랜치가 없을 때
```bash
git fetch origin dev
git worktree add -b "$BRANCH" "$WORKTREE_PATH" origin/dev
```

### Windows 경로 처리
Windows에서 `../hotfix-{티켓번호}` 경로:
- bash: `../hotfix-INC-42`
- 실제 경로: `C:\geumpumta\hotfix-INC-42`

---

## Phase 5 — PR 생성 엣지케이스

### gh CLI 미인증 시
```bash
gh auth status  # 인증 상태 확인
gh auth login   # 재인증
```

### 커밋이 없어 PR 생성 불가 시
핫픽스 작업이 완료되지 않은 경우 → Phase 4로 돌아가 작업 완료 후 재시도.

### PR 생성 후 Jira 업데이트 실패 시
Atlassian MCP로 코멘트 추가 실패하면 PR URL을 출력하고 수동 업데이트 안내.

---

## Phase 6 — 회고 문서 엣지케이스

### Notion MCP 미연결 시
`docs/incidents/` 디렉토리에 로컬 저장:
```bash
mkdir -p docs/incidents
# YYYY-MM-DD-{티켓번호}.md 파일 생성
```

### 장애 해결 시간 불명확 시
5-Why 분석에서 근본 원인이 불명확하면:
- "추가 분석 필요" 섹션 추가
- Action Items에 "근본 원인 심층 분석" 태스크 포함

### 재발 방지 대책 — Geumpumta 공통 패턴
| 장애 유형 | 공통 재발 방지 대책 |
|----------|------------------|
| 캐시 관련 | 캐시 eviction 테스트 케이스 추가 |
| DB 연결 | HikariCP pool 사이즈 + 타임아웃 설정 검토 |
| 시즌 전환 | `SeasonRankingSnapshot` Retry 로직 검증 강화 |
| FCM 장애 | FCM 실패 시 재시도 로직 / 알림 fallback 검토 |
| 인스턴스 다운 | Grafana Alert 쿼리 정확성 검증 (NoData 방지) |
