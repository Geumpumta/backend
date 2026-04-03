# Grafana 쿼리 모음 — Geumpumta

datasourceUid:
- Prometheus: `ff9b8fyp7herkf`
- Loki: `af9bh6f4lepz4a`

---

## Phase 1에서 사용할 PromQL 쿼리

### 인스턴스 상태
```promql
# prod 인스턴스 UP 여부 (0=다운, 1=정상)
up{job="geumpumta-backend-prod"}

# 전체 인스턴스 상태 한눈에
up{job=~"geumpumta-backend.*"}
```

### HTTP 에러율
```promql
# 5xx 에러율 (1분 평균)
rate(http_server_requests_seconds_count{job="geumpumta-backend-prod", status=~"5.."}[1m])

# 4xx 에러율
rate(http_server_requests_seconds_count{job="geumpumta-backend-prod", status=~"4.."}[1m])

# 전체 에러율 %
sum(rate(http_server_requests_seconds_count{job="geumpumta-backend-prod", status=~"[45].."}[5m]))
/ sum(rate(http_server_requests_seconds_count{job="geumpumta-backend-prod"}[5m])) * 100
```

### 응답 시간 (Latency)
```promql
# p99 응답시간 (초)
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket{job="geumpumta-backend-prod"}[5m])) by (le, uri)
)

# p95 응답시간
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket{job="geumpumta-backend-prod"}[5m])) by (le)
)
```

### JVM / 메모리
```promql
# JVM 힙 사용률 %
jvm_memory_used_bytes{job="geumpumta-backend-prod", area="heap"}
/ jvm_memory_max_bytes{job="geumpumta-backend-prod", area="heap"} * 100

# GC pause 횟수 증가율
rate(jvm_gc_pause_seconds_count{job="geumpumta-backend-prod"}[5m])
```

### DB 커넥션 풀 (HikariCP)
```promql
# 활성 커넥션 수
hikaricp_connections_active{job="geumpumta-backend-prod"}

# 대기 중인 커넥션 수 (높으면 DB 병목)
hikaricp_connections_pending{job="geumpumta-backend-prod"}

# 커넥션 획득 타임아웃 횟수
hikaricp_connections_timeout_total{job="geumpumta-backend-prod"}
```

### Redis
```promql
# Redis 연결 상태
up{job="redis"}

# Redis 메모리 사용량
redis_memory_used_bytes
```

### MySQL
```promql
# MySQL 연결 상태
up{job="mysql"}

# 슬로우 쿼리 발생률
rate(mysql_global_status_slow_queries[5m])

# 활성 커넥션 수
mysql_global_status_threads_connected
```

---

## Loki LogQL 쿼리

### 에러 로그 검색 (최근 30분)
```logql
{job="geumpumta-backend-prod"} |= "ERROR" | line_format "{{.message}}"
```

### 특정 예외 검색
```logql
# BusinessException 계열
{job="geumpumta-backend-prod"} |= "BusinessException"

# Spring 예외
{job="geumpumta-backend-prod"} |= "Exception" |= "ERROR"

# DB 관련 에러
{job="geumpumta-backend-prod"} |~ "DataAccessException|SQLSyntaxErrorException|HikariPool"
```

### 에러 코드별 검색
```logql
# 시즌(SE) 에러
{job="geumpumta-backend-prod"} |= "SE0"

# 학습세션(ST) 에러
{job="geumpumta-backend-prod"} |= "ST0"

# FCM 에러
{job="geumpumta-backend-prod"} |= "F0"
```

### 에러 발생 빈도 집계
```logql
sum by (level) (
  count_over_time({job="geumpumta-backend-prod"} |= "ERROR" [5m])
)
```

---

## 장애 유형별 추천 쿼리 조합

| 장애 유형 | 우선 확인 쿼리 |
|----------|--------------|
| 인스턴스 다운 | `up{job="geumpumta-backend-prod"}` |
| 에러율 급증 | HTTP 5xx 에러율 + Loki ERROR 패턴 |
| 응답 지연 | p99 latency + HikariCP pending + 슬로우 쿼리 |
| 메모리 이슈 | JVM 힙 사용률 + GC pause |
| DB 연결 장애 | `up{job="mysql"}` + HikariCP timeout + Loki DB 에러 |
| Redis 장애 | `up{job="redis"}` + 캐시 관련 로그 |
| 시즌 전환 오류 | SE 에러 코드 + SeasonService 로그 |
| FCM 장애 | F 에러 코드 + FCM 관련 로그 |
