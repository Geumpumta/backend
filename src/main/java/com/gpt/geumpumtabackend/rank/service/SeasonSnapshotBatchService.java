package com.gpt.geumpumtabackend.rank.service;

import com.gpt.geumpumtabackend.rank.domain.SeasonRankingSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SeasonSnapshotBatchService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int saveBatchWithJdbc(List<SeasonRankingSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT INTO season_ranking_snapshot
            (season_id, user_id, rank_type, final_rank, final_total_millis,
             department, snapshot_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        int batchSize = 2000;
        int totalSaved = 0;

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < snapshots.size(); i += batchSize) {
            int end = Math.min(i + batchSize, snapshots.size());
            List<SeasonRankingSnapshot> batch = snapshots.subList(i, end);

            int[][] updateCounts = jdbcTemplate.batchUpdate(sql, batch, batchSize,
                (ps, snapshot) -> {
                    ps.setLong(1, snapshot.getSeasonId());
                    ps.setLong(2, snapshot.getUserId());
                    ps.setString(3, snapshot.getRankType().name());
                    ps.setInt(4, snapshot.getFinalRank());
                    ps.setLong(5, snapshot.getFinalTotalMillis());
                    if (snapshot.getDepartment() != null) {
                        ps.setString(6, snapshot.getDepartment().name());
                    } else {
                        ps.setNull(6, java.sql.Types.VARCHAR);
                    }
                    ps.setTimestamp(7, Timestamp.valueOf(snapshot.getSnapshotAt()));
                    ps.setTimestamp(8, Timestamp.valueOf(now));
                    ps.setTimestamp(9, Timestamp.valueOf(now));
                });

            for (int[] batchUpdateCounts : updateCounts) {
                totalSaved += batchUpdateCounts.length;
            }
        }
        return totalSaved;
    }
}
