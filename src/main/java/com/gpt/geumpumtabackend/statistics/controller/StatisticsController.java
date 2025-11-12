package com.gpt.geumpumtabackend.statistics.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.statistics.api.StatisticsApi;
import com.gpt.geumpumtabackend.statistics.dto.response.DailyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.GrassStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.MonthlyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.WeeklyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/statistics")
public class StatisticsController implements StatisticsApi {

    private final StatisticsService statisticsService;

    @GetMapping("/day")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<DailyStatisticsResponse>> getDailyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Long userId
    ) {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                statisticsService.getDailyStatistics(date, userId))
        );
    }

    @GetMapping("/week")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<WeeklyStatisticsResponse>> getWeeklyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Long userId
    ) {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                statisticsService.getWeeklyStatistics(date, userId))
        );
    }

    @GetMapping("/month")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<MonthlyStatisticsResponse>> getMonthlyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                statisticsService.getMonthlyStatistics(date, userId)
        ));
    }

    @GetMapping("/grass")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<GrassStatisticsResponse>> getGrassStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                statisticsService.getGrassStatistics(date, userId)
        ));
    }
}
