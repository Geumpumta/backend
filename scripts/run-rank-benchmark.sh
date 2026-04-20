#!/usr/bin/env bash
# Orchestration wrapper for MySQL vs Redis ranking benchmark.
#
# Prerequisites:
#   - docker-compose up -d         (MySQL 3311, Redis 6379)
#   - ./gradlew bootRun --args='--spring.profiles.active=local'  (running in another shell)
#   - pip install matplotlib
#
# Usage:
#   ./scripts/run-rank-benchmark.sh                   # 기본: 전체 파이프라인 실행
#   BASE=http://localhost:8080 ./scripts/run-rank-benchmark.sh
#   WARMUP=10 ITER=100 USER_ID=1 ./scripts/run-rank-benchmark.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
WARMUP="${WARMUP:-20}"
ITER="${ITER:-200}"
USER_ID="${USER_ID:-1}"

echo "[1/4] /generate — 세션 데이터 적재"
curl -sf -X POST "$BASE/api/v1/rank-benchmark/generate" --max-time 1800 | tee /tmp/gen.json
echo

echo "[2/4] /backfill — Redis ZSET 적재"
curl -sf -X POST "$BASE/api/v1/rank-benchmark/backfill" --max-time 600 | tee /tmp/backfill.json
echo

echo "[3/4] /run — 벤치마크 측정 (warmup=$WARMUP, iter=$ITER, userId=$USER_ID)"
curl -sf -X POST "$BASE/api/v1/rank-benchmark/run" \
    -H 'Content-Type: application/json' \
    -d "{\"warmup\":$WARMUP,\"iterations\":$ITER,\"userId\":$USER_ID}" \
    --max-time 1800 | tee /tmp/run.json
echo

echo "[4/4] 그래프 생성"
python scripts/plot_rank_benchmark.py
