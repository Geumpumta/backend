# 장애 대응 템플릿 모음

---

## Jira 티켓 템플릿 (Phase 3)

**제목 형식:**
```
[INCIDENT] {장애 유형} - {영향 범위} ({YYYY-MM-DD})
```

**설명 본문:**
```
## 장애 개요
- 발생 일시: {발생 시각}
- 감지 일시: {감지 시각}
- 심각도: Critical / High / Medium
- 영향 범위: {prod/dev, 영향받는 기능}

## 증상
{사용자 또는 모니터링에서 감지된 증상}

## 원인 분석
{Phase 1 Grafana 분석 결과}

추정 원인:
1. {원인 후보 1}
2. {원인 후보 2}

감지된 에러 코드: {ExceptionType 코드}

## 관련 코드
{Phase 2 코드 분석 결과 — 영향 도메인, 핵심 파일, 문제 지점}

## 조치 계획
- [ ] 핫픽스 브랜치 생성: hotfix/{티켓번호}-{설명}
- [ ] 코드 수정
- [ ] 테스트 (단위 + 통합)
- [ ] PR → dev 머지
- [ ] 운영 배포 확인
- [ ] 회고 문서 작성

## 참고 링크
- Grafana: https://geumpumta.shop/grafana
- PR: (생성 후 업데이트)
```

---

## PR Body 템플릿 (Phase 5)

```markdown
## 관련 이슈
Closes {Jira 티켓 URL}

## 장애 요약
{장애 유형과 영향 범위 한 줄 요약}

## 원인
{근본 원인 설명}

## 변경 내용
- {변경 항목 1}
- {변경 항목 2}

## 테스트 체크리스트
- [ ] 단위 테스트 통과 (`./gradlew test --tests "{테스트클래스명}"`)
- [ ] 통합 테스트 통과 (`./gradlew test`)
- [ ] 로컬 환경 수동 검증
- [ ] Grafana 에러율 정상 복귀 확인

## 배포 시 주의사항
{캐시 eviction 필요 여부, DB 마이그레이션 여부, 재시작 필요 여부 등}

## 스크린샷 / 로그 (선택)
{Grafana 대시보드 캡처 또는 수정 전/후 로그}

---
🤖 Generated with [Claude Code](https://claude.com/claude-code)
Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

---

## 회고 문서 템플릿 (Phase 6)

```markdown
# 장애 회고: {티켓번호} — {제목}

> 작성일: {YYYY-MM-DD} | 작성자: {담당자}

---

## 기본 정보

| 항목 | 내용 |
|------|------|
| 발생 일시 | {YYYY-MM-DD HH:mm} KST |
| 감지 일시 | {YYYY-MM-DD HH:mm} KST |
| 해결 일시 | {YYYY-MM-DD HH:mm} KST |
| 총 장애 시간 | {N시간 N분} |
| 영향 범위 | {prod/dev, 영향받은 기능} |
| 심각도 | Critical / High / Medium |

---

## 장애 타임라인

| 시각 (KST) | 이벤트 |
|-----------|--------|
| HH:mm | Grafana Alert 발화 |
| HH:mm | 장애 인지 및 분석 시작 |
| HH:mm | 원인 특정 |
| HH:mm | 핫픽스 작업 시작 |
| HH:mm | PR 생성 및 리뷰 |
| HH:mm | 배포 완료 |
| HH:mm | 정상화 확인 |

---

## 원인 분석 (5-Why)

- **Why 1:** {첫 번째 왜}
- **Why 2:** {두 번째 왜}
- **Why 3:** {세 번째 왜}
- **Why 4:** {네 번째 왜}
- **Why 5 (근본 원인):** {최종 원인}

---

## 조치 내용

### 즉시 조치
{장애 확산 방지를 위해 취한 즉각적인 조치}

### 핫픽스 내용
{코드 변경 내용 요약}

- PR: {PR URL}
- 변경 파일: {파일 목록}

---

## 재발 방지 대책

| 항목 | 유형 | 담당 | 기한 |
|------|------|------|------|
| {모니터링 개선} | 모니터링 | {담당자} | {YYYY-MM-DD} |
| {테스트 보강} | 테스트 | {담당자} | {YYYY-MM-DD} |
| {코드 개선} | 개발 | {담당자} | {YYYY-MM-DD} |
| {프로세스 개선} | 프로세스 | {담당자} | {YYYY-MM-DD} |

---

## 참고 자료

- Jira 티켓: {티켓 URL}
- PR: {PR URL}
- Grafana (장애 시점): https://geumpumta.shop/grafana
- 관련 로그: {로그 링크}
```

---

## Notion MCP 연결 방법 (미연결 시 안내)

Notion MCP가 없을 경우 사용자에게 아래 안내를 출력한다:

```
## Notion MCP 연결 방법

1. Notion Integration 생성:
   https://www.notion.so/my-integrations → 새 Integration 생성

2. Claude Code에 Notion MCP 추가:
   claude mcp add notion "npx @notionhq/notion-mcp-server" -s user \
     -e NOTION_API_KEY="{발급받은 토큰}"

3. Notion 페이지에 Integration 권한 부여:
   장애 회고를 저장할 페이지 → ... → Connections → Integration 추가

연결 전까지는 로컬 파일로 저장합니다:
→ docs/incidents/{YYYY-MM-DD}-{티켓번호}.md
```
