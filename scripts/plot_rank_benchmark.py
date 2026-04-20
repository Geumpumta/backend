#!/usr/bin/env python3
"""
Plot MySQL vs Redis ranking benchmark results.

사용법:
    python scripts/plot_rank_benchmark.py
        → benchmark-results/rank/ 의 가장 최근 CSV 자동 사용

    python scripts/plot_rank_benchmark.py path/to/rank-bench-*.csv
        → 지정한 CSV 사용

요구 패키지:
    pip install matplotlib
"""
from __future__ import annotations

import csv
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

DEFAULT_DIR = Path(__file__).resolve().parent.parent / "benchmark-results" / "rank"

MYSQL_COLOR = "#1f77b4"
REDIS_COLOR = "#d62728"


def latest_csv() -> Path:
    files = sorted(DEFAULT_DIR.glob("rank-bench-*.csv"))
    if not files:
        raise SystemExit(f"[ERR] CSV 없음 — {DEFAULT_DIR} 비어있음. 먼저 벤치마크를 실행하세요.")
    return files[-1]


def load(csv_path: Path) -> list[dict]:
    with csv_path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    numeric = {
        "mysql_avg_ms", "mysql_p50_ms", "mysql_p95_ms", "mysql_p99_ms",
        "mysql_min_ms", "mysql_max_ms",
        "redis_avg_ms", "redis_p50_ms", "redis_p95_ms", "redis_p99_ms",
        "redis_min_ms", "redis_max_ms",
        "mysql_over_redis_ratio",
    }
    for row in rows:
        for k in list(row.keys()):
            if k in numeric:
                row[k] = float(row[k])
    return rows


def grouped_bars(ax, scenarios, mysql_vals, redis_vals, title, ylabel):
    x = np.arange(len(scenarios))
    width = 0.38
    ax.bar(x - width / 2, mysql_vals, width, label="MySQL", color=MYSQL_COLOR)
    ax.bar(x + width / 2, redis_vals, width, label="Redis", color=REDIS_COLOR)
    ax.set_xticks(x)
    ax.set_xticklabels(scenarios, rotation=30, ha="right", fontsize=9)
    ax.set_ylabel(ylabel)
    ax.set_yscale("log")
    ax.set_title(title)
    ax.grid(True, axis="y", which="both", alpha=0.3)
    ax.legend(loc="best", fontsize=9)

    for i, (m, r) in enumerate(zip(mysql_vals, redis_vals)):
        ax.text(i - width / 2, m, f"{m:.2f}", ha="center", va="bottom", fontsize=7, color=MYSQL_COLOR)
        ax.text(i + width / 2, r, f"{r:.2f}", ha="center", va="bottom", fontsize=7, color=REDIS_COLOR)


def plot(csv_path: Path) -> Path:
    rows = load(csv_path)
    scenarios = [r["scenario"] for r in rows]

    mysql_avg = [r["mysql_avg_ms"] for r in rows]
    redis_avg = [r["redis_avg_ms"] for r in rows]
    mysql_p95 = [r["mysql_p95_ms"] for r in rows]
    redis_p95 = [r["redis_p95_ms"] for r in rows]
    mysql_p99 = [r["mysql_p99_ms"] for r in rows]
    redis_p99 = [r["redis_p99_ms"] for r in rows]
    ratios = [r["mysql_over_redis_ratio"] for r in rows]

    fig, axes = plt.subplots(2, 2, figsize=(16, 9))
    ax_avg, ax_p95 = axes[0]
    ax_p99, ax_ratio = axes[1]

    grouped_bars(ax_avg, scenarios, mysql_avg, redis_avg,
                 "Average latency (ms, log scale)", "avg ms")
    grouped_bars(ax_p95, scenarios, mysql_p95, redis_p95,
                 "p95 latency (ms, log scale)", "p95 ms")
    grouped_bars(ax_p99, scenarios, mysql_p99, redis_p99,
                 "p99 latency (ms, log scale)", "p99 ms")

    bar_colors = [REDIS_COLOR if r > 1 else MYSQL_COLOR for r in ratios]
    x = np.arange(len(scenarios))
    ax_ratio.bar(x, ratios, color=bar_colors)
    ax_ratio.axhline(1.0, color="black", linewidth=0.8)
    ax_ratio.set_xticks(x)
    ax_ratio.set_xticklabels(scenarios, rotation=30, ha="right", fontsize=9)
    ax_ratio.set_ylabel("mysql_avg / redis_avg")
    ax_ratio.set_title("Speed ratio (>1 → Redis faster)")
    ax_ratio.grid(True, axis="y", alpha=0.3)
    for i, r in enumerate(ratios):
        ax_ratio.text(i, r, f"{r:.2f}x", ha="center", va="bottom", fontsize=8)

    fig.suptitle(f"MySQL vs Redis ranking benchmark\n{csv_path.name}", fontsize=13)
    fig.tight_layout(rect=[0, 0, 1, 0.96])

    out_path = csv_path.with_suffix(".png")
    fig.savefig(out_path, dpi=120)
    plt.close(fig)
    return out_path


def main() -> None:
    csv_path = Path(sys.argv[1]) if len(sys.argv) > 1 else latest_csv()
    if not csv_path.exists():
        raise SystemExit(f"[ERR] CSV 파일 없음: {csv_path}")
    out = plot(csv_path)
    print(f"[OK] saved: {out}")


if __name__ == "__main__":
    main()
