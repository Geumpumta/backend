# 일간 랭킹 RPS 실험

날짜 파라미터 없는 `GET /api/v1/rank/personal/daily`에서 요청 시 MySQL 집계와 Redis 점수 조회를 비교한다. Redis 경로에도 MySQL 프로필 조회가 포함된다. 가설은 동일 데이터에서 Redis 점수 조회가 클라이언트 p99, MySQL CPU와 커넥션 대기를 줄인다는 것이다.

## 장비와 시작 조건

- 현재 Windows PC를 Docker/WSL 2 서버로 쓰고 별도 물리 PC를 k6 발생기로 사용한다. 같은 LAN에서 시계 동기화와 유휴 RTT를 확인한다. OS와 CPU/RAM, 앱 빌드·이미지 버전, DB 인덱스와 설정, WSL 메모리·스왑 상태를 기록한다.
- 로컬 MySQL·Redis 인프라를 먼저 띄운다. 기본 Compose 네트워크는 `backend_appnet`이다. 다른 이름이면 양쪽 Compose에 `RANK_DB_NETWORK`를 동일하게 지정한다. 기존 DB 및 Redis 포트는 테스트 LAN에서 접근하지 못하도록 제한한다. Windows 방화벽에서 앱 TCP 8080을 부하 발생기 IP에만 허용하고 실제 접속 범위를 확인한 다음 실행한다.
- Java 21로 같은 실행 JAR을 준비한다. `RANK_APP_JAR`에 절대 경로, `RANK_APP_CONFIG`에 승인된 실험용 Spring YAML의 절대 경로, `RANK_BIND_IP`에 서버의 실험 LAN IPv4를 지정한다. 외부 YAML로 실제 앱 부팅에 필요한 설정과 실험 DB 접근 계정, JPA 스키마 초기화 설정을 제공한다. 앱은 내부 Docker DNS로 MySQL·Redis에 접속한다. 토큰이나 설정 파일은 저장소에 넣지 않는다.
- 운영 데이터와 외부 서비스를 사용하지 않는 새 실험 스키마 `rank_lab_10000`을 만들고 `RANK_SCHEMA`를 지정한다. 앱으로 스키마를 초기화하고 일반 사용자 한 명을 정상 가입시킨다. 세션이 없는 것을 확인하고 MySQL 배치 클라이언트로 `seed.sql`을 적용한다. `--force`를 사용하지 않는다. 시드는 03:00 이후 실행하고 같은 서울 날짜 안에 측정을 끝낸다. 인스턴스를 독점하고 필요한 예약·쓰기 작업이 없는 시간대를 고른다. 앱 실험 컨테이너는 자동 종료 한도를 24시간으로 유지한다.
- 시드 결과는 사용자 10,000명, 과거 종료 세션 300,000건, 당일 종료 20,000건, 진행 중 500건이다. 1,000명과 50,000명 실험에는 각각 `rank_lab_1000`, `rank_lab_50000`을 같은 방법으로 준비한다.

```powershell
docker compose -f tools/rank-read/compose.yml up -d
$env:RANK_PROFILES = 'rank-read-lab'
docker compose -f tools/rank-read/app.compose.yml up -d rank-app
# Redis 모드 전환
$env:RANK_PROFILES = 'rank-read-lab,rank-read-redis'
docker compose -f tools/rank-read/app.compose.yml up -d rank-app
```

같은 프로필로 다음 시도를 시작할 때는 `docker compose -f tools/rank-read/app.compose.yml restart rank-app`으로 앱을 재시작한다. JVM 힙은 512 MiB–1 GiB, Tomcat 최대 스레드는 100개, Hikari 최대 커넥션은 16개다. 측정 직전에 인증된 랭킹 API 1건에서 HTTP 200와 정상 랭킹 응답을 확인한다. Redis 모드에서 이 호출로 점수를 초기화한다.

외부 서비스 없이 만든 합성 실행 설정을 사용할 때는 양쪽 모드에 `test` 프로필도 추가해 Firebase 초기화를 제외한다. 인증은 실제 앱 필터와 테스트 전용 사용자 세션으로 처리한다. 같은 토큰을 사용하면 요청마다 동일 세션의 마지막 접근 시간이 갱신되므로, 세션 갱신 SQL도 별도로 기록하고 두 조회 경로의 공통 비용으로 해석한다.

Grafana의 **Daily rank read**와 Prometheus는 서버의 루프백에 둔다. 부하 발생기에서 허용된 서버 SSH 계정으로 `ssh -N -L 19090:127.0.0.1:9090 <계정>@<서버IP>`를 실행한다. 터널은 Prometheus 지표 전송에만 사용한다. k6 요청은 앱의 LAN 주소로 직접 보낸다. `/targets`에서 rank-app과 rank-containers가 UP인지 확인한다.

## 실행과 판정

### GitHub-hosted runner

GitHub의 `rank-read-lab` Environment에 변수 `RANK_VPN_IP`(서버의 Tailscale IPv4)와 Secrets `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET`, `RANK_TOKEN_B64`를 설정한다. `RANK_TOKEN_B64`에는 JWT 원본 바이트를 표준 Base64로 인코딩한 값을 넣는다. Tailscale에서 임시 러너 태그 `tag:rank-k6`를 발급할 수 있는 OAuth 클라이언트를 만들고 해당 태그에서 실험 서버의 TCP 8080에만 접근하도록 제한한다. 서버의 `RANK_BIND_IP`에는 Tailscale IPv4를 지정한다. GitHub에서 실행 가능한 브랜치에 워크플로와 k6 스크립트가 반영되어 있어야 한다. [Tailscale GitHub Actions 연동](https://tailscale.com/docs/solutions/connect-github-cicd-workflows-to-private-infrastructure-without-public-exposure)

서버에서 조회 모드를 직접 적용한 다음 **Actions → Rank read load test → Run workflow**에서 같은 모드와 요청률을 선택한다. 한 실행은 한 조합만 측정하며 네 조합을 순차로 호출한다. 모드 전환은 서버에서 관리한다. 실행마다 러너 VM이 달라질 수 있으므로 아티팩트의 VPN 연결 방식, 연결 시간, 러너 CPU·메모리 요약을 비교한다. 기준에 미달한 경우에도 k6가 생성한 결과 JSON을 아티팩트에 저장한다.

기본 브랜치에 워크플로가 아직 없다면 실험 브랜치 `perf/daily-ranking-redis-read`에서 `tools/rank-read/hosted-run.txt`를 한 줄 `mysql 200` 형식으로 갱신해 push한다. 해당 파일을 바꾸는 실험 브랜치 push만 측정을 시작한다. 서버 모드를 먼저 맞추고 이전 실행이 끝난 뒤 다음 조합을 요청한다. 연결 실패로 같은 조합을 재시도할 때는 GitHub Actions에서 해당 실행을 다시 실행한다.

기본값에서 Prometheus는 로컬 앱·컨테이너 지표를 계속 수집하고 k6 지표는 JSON으로 저장한다. k6 실시간 전송도 필요하면 Prometheus TCP 9090을 서버 Tailscale 주소에서 해당 러너 태그에만 허용한 뒤 Environment 변수 `RANK_PROMETHEUS_ENABLED=true`를 지정한다. GitHub의 실행 시각을 JSON의 UTC 측정 구간으로 가져와 Grafana 구간과 맞춘다.

10,000명으로 200 RPS MySQL → Redis, 400 RPS Redis → MySQL을 각 1회 순서대로 측정한다. 각 조합에서 모드를 확인하고 다음 명령의 인자를 바꿔 실행한다. 토큰은 숨김 프롬프트에 입력한다.

```powershell
./tools/rank-read/run.ps1 -Mode mysql -Rate 200 -Users 10000 -BaseUrl "http://${serverIp}:8080" -PrometheusUrl http://127.0.0.1:19090
```

각 실행은 30초 워밍업, 5초 정리, 120초 고정 도착률 측정으로 진행한다. 사전 할당 VU는 목표 RPS, 최대 VU는 그 두 배, 요청 타임아웃은 5초다. 예정 건수 전부를 발행·완료하고 종단 p99 ≤ 200ms, 오류율 ≤ 0.1%이면 통과한다. 성공 RPS는 정상 HTTP 200 및 랭킹 응답 건수를 120초로 나눈다. 발행 누락이나 미완료가 있으면 목표 부하 미검증으로 기록한다. k6의 기준 위반 종료 후에도 JSON 보고서를 보관한다.

- 방식별로 400 통과 시 600 → 800 → 1,000 RPS까지 올려 첫 기준 미달에서 멈춘다. 200 통과·400 미달이면 300, 200 미달이면 100을 측정한다. 600 이상 첫 미달이면 직전 통과와의 중간값을 한 번 측정한다. 발생기나 LAN 한계로 목표량이 공급되지 않은 시도에서는 서버 경계 판정을 보류한다.
- 데이터 규모의 영향은 별도로 확인한다. 필요하면 200 RPS를 유지하고 1,000명과 50,000명에서 두 방식 각각 한 번씩 실행한다. 10,000명 값은 1차 결과를 사용한다.

## 관측과 결과

- **클라이언트·호스트** — 발행·성공 RPS, 누락, 활성 VU, 연결·전송 지연과 k6 프로세스 CPU를 본다. 서버 Windows CPU·가용 메모리와 WSL 스왑도 같은 구간에 기록한다.
- **MySQL·Redis** — MySQL CPU와 Hikari 대기·획득 지연을 본다. 양쪽 측정 구간의 직전·직후에 실험 스키마를 선택하고 `sql-metrics.sql`을 실행한다. 집계와 프로필 조회 digest별 `SUM_TIMER_WAIT` 차이를 `COUNT_STAR` 차이로 나눠 평균을 구한다. 타이머 단위는 피코초이며 ms 환산은 10⁹으로 나눈다. Redis는 컨테이너 CPU와 `INFO commandstats`의 호출 횟수 및 누적 실행 µs 차이를 기록한다. 초기화와 워밍업은 최종 판정에 포함하지 않는다. 카운터가 초기화되거나 구간을 맞출 수 없으면 해당 비교는 미확정으로 남긴다.
- **앱** — Spring 요청 p99, Tomcat 사용 스레드, JVM GC를 본다. 컨테이너의 `/build/rank-read-tomcat/logs`에서 `X-Rank-Run`과 `measure` 단계로 로그를 대조한다.
- **보고** — `build/rank-read`의 실행 JSON에 측정 시작, 발행 구간 끝, 마지막 완료 시각이 기록된다. `compare.ps1 -Report <JSON 경로 목록>`은 사용자 규모와 RPS별로 묶고 각 모드의 유효한 단일 결과가 있을 때 p99 개선율을 계산한다. 결과표에 발행·성공 RPS, 평균, p99, 오류율, 누락, 판정을 적고 Grafana 시간창을 첨부한다. 각 경로의 최고 통과와 첫 미달, 그리고 가장 먼저 변한 지표를 제시한다. 1,000 RPS까지 통과하면 그 범위까지만 확인한 것으로 기록한다.
