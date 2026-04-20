package com.gpt.geumpumtabackend.rankbenchmark.controller;

import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.rankbenchmark.mysql.MysqlRankingReader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rank-benchmark/mysql")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "benchmark.rank", name = "enabled", havingValue = "true")
public class MysqlRankingBenchmarkController {

    private final MysqlRankingReader reader;

    @GetMapping("/personal/daily")
    public ResponseEntity<PersonalRankingResponse> personalDaily(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(reader.personalDaily(userId));
    }

    @GetMapping("/personal/weekly")
    public ResponseEntity<PersonalRankingResponse> personalWeekly(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(reader.personalWeekly(userId));
    }

    @GetMapping("/personal/monthly")
    public ResponseEntity<PersonalRankingResponse> personalMonthly(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(reader.personalMonthly(userId));
    }

    @GetMapping("/department/daily")
    public ResponseEntity<DepartmentRankingResponse> deptDaily(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(reader.departmentDaily(userId));
    }

    @GetMapping("/department/weekly")
    public ResponseEntity<DepartmentRankingResponse> deptWeekly(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(reader.departmentWeekly(userId));
    }

    @GetMapping("/department/monthly")
    public ResponseEntity<DepartmentRankingResponse> deptMonthly(@RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(reader.departmentMonthly(userId));
    }
}
