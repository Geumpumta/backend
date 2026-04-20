package com.gpt.geumpumtabackend.rankbenchmark.service;

import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingEntryResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class RankingResultVerifier {

    private static final long TOLERANCE_MS = 500;
    private static final int TOP_K_STRICT = 5;

    public boolean comparePersonal(String scenario, PersonalRankingResponse mysql, PersonalRankingResponse redis) {
        List<PersonalRankingEntryResponse> m = mysql.topRanks();
        List<PersonalRankingEntryResponse> r = redis.topRanks();
        int limit = Math.min(TOP_K_STRICT, Math.min(m.size(), r.size()));
        boolean ok = true;
        for (int i = 0; i < limit; i++) {
            PersonalRankingEntryResponse a = m.get(i);
            PersonalRankingEntryResponse b = r.get(i);
            if (!a.userId().equals(b.userId())) {
                log.warn("[RANK-BENCH][VERIFY:{}] top-{} user mismatch mysql={} redis={}",
                        scenario, i + 1, a.userId(), b.userId());
                ok = false;
                continue;
            }
            long diff = Math.abs(a.totalMillis() - b.totalMillis());
            if (diff > TOLERANCE_MS) {
                log.warn("[RANK-BENCH][VERIFY:{}] top-{} user={} millis diff={}ms > tolerance",
                        scenario, i + 1, a.userId(), diff);
                ok = false;
            }
        }
        return ok;
    }

    public boolean compareDepartment(String scenario, DepartmentRankingResponse mysql, DepartmentRankingResponse redis) {
        List<DepartmentRankingEntryResponse> m = mysql.topRanks();
        List<DepartmentRankingEntryResponse> r = redis.topRanks();
        int limit = Math.min(TOP_K_STRICT, Math.min(m.size(), r.size()));
        boolean ok = true;
        for (int i = 0; i < limit; i++) {
            DepartmentRankingEntryResponse a = m.get(i);
            DepartmentRankingEntryResponse b = r.get(i);
            if (!a.departmentName().equals(b.departmentName())) {
                log.warn("[RANK-BENCH][VERIFY:{}] dept-{} name mismatch mysql={} redis={}",
                        scenario, i + 1, a.departmentName(), b.departmentName());
                ok = false;
            }
        }
        return ok;
    }
}
