---
name: incident-response
description: >
  Geumpumta 백엔드 장애 대응 자동화 스킬. 장애 탐지부터 회고 문서화까지 6단계 전 과정을 통합 처리한다.
  "장애 발생", "에러 분석해줘", "인시던트 대응", "서버 다운", "알람 울렸어", "/ir", "/incident-response",
  "Grafana alert 확인", "핫픽스 브랜치 만들어줘", "장애 티켓 생성", "장애 회고 작성" 등의 요청 시 반드시 이 스킬을 사용할 것.
  특정 단계만 실행하려면 "phase=N" 또는 "2단계부터" 형식으로 시작 지점을 지정할 수 있다.
---

# Incident Response — Geumpumta 장애 대응 가이드

장애 발생 시 **6개 Phase**를 순서대로 실행한다. 각 Phase 완료 후 사용자에게 다음 단계 진행 여부를 확인한다.
특정 Phase만 실행할 때는 해당 Phase로 바로 점프한다.

> 각 Phase의 상세 쿼리·템플릿은 아래 레퍼런스 파일에서 필요할 때만 읽어라:
> - `references/grafana-queries.md` — PromQL / LogQL 쿼리 모음 (Phase 1에서 읽기)
> - `references/templates.md` — Jira 티켓 · PR Body · 회고 문서 템플릿 (Phase 3, 5, 6에서 읽기)
> - `references/phase-guide.md` — 각 Phase 판단 기준 및 엣지케이스 처리 (막힐 때 읽기)

---

## 실행 전 확인 사항

사용자가 시작 Phase를 명시하지 않으면 Phase 1부터 시작한다.
`phase=N` 또는 "N단계부터" 형식이면 해당 Phase로 바로 점프한다.

장애 컨텍스트(에러 메시지, 알람 내용, 증상)가 있으면 Phase 1 분석에 활용한다.

---

## Phase 1 — 장애 탐지 & 원인 분석

**`[Phase 1/6] 장애 탐지 중...`** 출력 후 시작.

`references/grafana-queries.md`를 읽어 적절한 쿼리를 선택한다.

### 순서

1. **Firing Alert 조회** — `mcp__grafana__alerting_manage_rules` (operation: list, states: ["firing", "error", "pending"])
2. **핵심 메트릭 조회** — `mcp__grafana__query_prometheus` 로 에러율, 응답시간, `up` 상태 확인
3. **로그 에러 패턴** — `mcp__grafana__query_loki_logs` 로 최근 30분 ERROR/WARN 로그 검색
4. **자연어 요약** — 아래 형식으로 분석 결과 출력:

```
## 장애 분석 결과
- 장애 유형: (인스턴스 다운 / 에러율 급증 / 응답 지연 / DB 연결 불가 / ...)
- 영향 범위: (prod / dev, 영향받는 엔드포인트 또는 기능)
- 추정 원인: (1~3개 후보, 근거 포함)
- 에러 코드: (감지된 ExceptionType 코드 — C001, ST001 등)
- 발생 시각: (첫 감지 시각)
- 심각도: Critical / High / Medium
```

Phase 1 완료 후 → "Phase 2(코드 분석)로 진행할까요?" 확인.

---

## Phase 2 — 코드베이스 컨텍스트 로딩

**`[Phase 2/6] 관련 코드 분석 중...`** 출력 후 시작.

Phase 1 결과의 에러 유형과 에러 코드를 기반으로 관련 도메인을 특정한다.

### 에러 코드 → 도메인 매핑
| 에러 접두사 | 도메인 경로 |
|------------|------------|
| ST (학습)  | `study/` |
| SE (시즌)  | `rank/` |
| U (사용자) | `user/` |
| T (토큰)   | `token/` |
| F (FCM)    | `fcm/` |
| B (게시판) | `board/` |
| W (WiFi)   | `wifi/` |
| C, S (공통)| `global/` |

### 로딩 순서

1. 해당 도메인의 `ExceptionType` enum — 에러 코드 의미 파악
2. 관련 `Service` 클래스 — 비즈니스 로직 및 트랜잭션 흐름
3. 관련 `Repository` — Native Query가 있으면 반드시 확인 (랭킹/통계 영향)
4. `CLAUDE.md` 핵심 규칙 재확인:
   - 시즌/캐시 관련이면 `activeSeason` 캐시 eviction 로직 확인
   - `StudySessionRepository` Native Query 수정 여부 확인

### 출력 형식
```
## 코드 컨텍스트
- 영향 도메인: (도메인명)
- 핵심 파일: (파일 경로 목록)
- 문제 가능 지점: (메서드명 + 이유)
- CLAUDE.md 관련 규칙: (해당하는 규칙)
- 권장 수정 방향: (구체적인 변경 제안)
```

Phase 2 완료 후 → "Phase 3(Jira 티켓 생성)으로 진행할까요?" 확인.

---

## Phase 3 — Jira 티켓 생성

**`[Phase 3/6] Jira 티켓 생성 중...`** 출력 후 시작.

`references/templates.md`의 Jira 티켓 템플릿을 읽어 사용한다.

Atlassian MCP를 사용해 Jira에 티켓을 생성한다.
- 프로젝트: Atlassian MCP에서 사용 가능한 프로젝트 조회 후 적절한 프로젝트 선택
- 이슈 타입: Bug
- 우선순위: Phase 1 심각도 기준 (Critical → Highest, High → High, Medium → Medium)
- 제목: `[INCIDENT] {장애 유형} - {영향 범위} ({발생 날짜})`
- 설명: Phase 1·2 분석 결과 포함

티켓 생성 후 **티켓 번호(예: INC-42)를 저장**한다. 이후 Phase 4, 5, 6에서 사용한다.

```
## Jira 티켓 생성 완료
- 티켓 번호: INC-{N}
- URL: https://cowngur5460.atlassian.net/browse/INC-{N}
- 우선순위: {우선순위}
```

Phase 3 완료 후 → "Phase 4(핫픽스 브랜치 생성)으로 진행할까요?" 확인.

---

## Phase 4 — Git Worktree 핫픽스 브랜치 생성

**`[Phase 4/6] 핫픽스 브랜치 생성 중...`** 출력 후 시작.

현재 작업 브랜치와 완전히 분리된 worktree를 생성한다.

### 실행 명령

```bash
# 브랜치명: hotfix/{티켓번호}-{간단설명} (영문 소문자, 하이픈)
# 예: hotfix/INC-42-season-cache-eviction

TICKET={티켓번호}
DESC={간단설명}  # Phase 2 수정 방향에서 도출
BRANCH="hotfix/${TICKET}-${DESC}"
WORKTREE_PATH="../hotfix-${TICKET}"

git worktree add -b "$BRANCH" "$WORKTREE_PATH" dev
```

worktree 생성 후 사용자에게 안내:

```
## 핫픽스 Worktree 생성 완료
- 브랜치: hotfix/{티켓번호}-{설명}
- 경로: ../hotfix-{티켓번호}/
- 베이스: dev 브랜치

작업 방법:
  cd ../hotfix-{티켓번호}   # 해당 디렉토리에서 작업
  # 수정 완료 후 "Phase 5로 진행" 입력

현재 브랜치({현재브랜치})는 영향 없음.
```

Phase 4 완료 후 → "핫픽스 작업이 완료되면 Phase 5(PR 생성)로 진행해 주세요." 안내.

---

## Phase 5 — PR 생성

**`[Phase 5/6] PR 생성 중...`** 출력 후 시작.

`references/templates.md`의 PR 템플릿을 읽어 사용한다.

```bash
cd "../hotfix-{티켓번호}"
gh pr create \
  --base dev \
  --title "[HOTFIX] {티켓번호} {장애 요약}" \
  --body "$(cat <<'EOF'
{PR 본문 — templates.md 참고}
EOF
)"
```

PR 생성 후:
1. PR URL을 Jira 티켓에 코멘트로 추가 (Atlassian MCP)
2. Jira 티켓 상태를 "In Progress" → "In Review"로 전환

```
## PR 생성 완료
- PR URL: {PR URL}
- Jira 업데이트: {티켓 번호} → In Review
```

Phase 5 완료 후 → "Phase 6(회고 문서 작성)으로 진행할까요?" 확인.

---

## Phase 6 — 회고 문서 자동 생성 & Notion 저장

**`[Phase 6/6] 회고 문서 작성 중...`** 출력 후 시작.

`references/templates.md`의 회고 문서 템플릿을 읽어 사용한다.

### Notion MCP 연결 상태 확인

Notion MCP가 연결되어 있으면 → Notion 페이지에 자동 저장.
연결되지 않았으면 → 마크다운 파일(`docs/incidents/YYYY-MM-DD-{티켓번호}.md`)로 로컬 저장 후 Notion 연결 방법 안내.

### 회고 문서 구조 (5-Why 기반)

```markdown
# 장애 회고: {티켓번호} — {제목}

## 기본 정보
- 발생 일시 / 감지 일시 / 해결 일시
- 영향 범위 / 심각도

## 장애 타임라인
| 시각 | 이벤트 |
|------|--------|

## 원인 분석 (5-Why)
- Why 1:
- Why 2:
- Why 3:
- Why 4:
- Why 5 (근본 원인):

## 조치 내용
- 즉시 조치 / 핫픽스 내용 / PR 링크

## 재발 방지 대책
| 항목 | 담당 | 기한 |
|------|------|------|

## 참고
- Jira: {티켓 URL}
- PR: {PR URL}
- Grafana: 장애 시점 대시보드 링크
```

---

## 전체 진행 상황 트래커

각 Phase 시작 시 전체 진행 상황을 한 줄로 표시한다:

```
진행: [1완료] → [2완료] → [3완료] → [4완료] → [5완료] → [6진행중]
```

---

## 중단 및 재개

사용자가 중간에 중단하면 현재까지의 컨텍스트(티켓 번호, 브랜치명, PR URL)를 요약해 저장한다.
재개 시 "어디서부터 이어갈까요?" 를 물어보고 해당 Phase로 점프한다.
