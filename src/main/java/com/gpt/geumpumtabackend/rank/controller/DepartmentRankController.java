package com.gpt.geumpumtabackend.rank.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.rank.api.DepartmentRankApi;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import com.gpt.geumpumtabackend.rank.service.DepartmentRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/rank/department")
@RequiredArgsConstructor
public class DepartmentRankController implements DepartmentRankApi {

    private final DepartmentRankService departmentRankService;

    /*
    학과 랭킹 일간 조회
     */
    @GetMapping("/daily")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<DepartmentRankingResponse>> getDailyRanking(Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date){
        DepartmentRankingResponse response;

        if (date == null) {
            // 현재 진행중인 일간 랭킹
            response = departmentRankService.getCurrentDailyDepartmentRanking(userId);
        } else {
            // 특정 날짜의 확정된 일간 랭킹
            response =  departmentRankService.getCompletedDailyDepartmentRanking(userId, date);
        }

        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    /*
    학과 랭킹 주간 조회
     */
    @GetMapping("/weekly")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<DepartmentRankingResponse>> getWeeklyRanking(Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date){
        DepartmentRankingResponse response;

        if (date == null) {
            // 현재 진행중인 주간 랭킹
            response = departmentRankService.getCurrentWeeklyDepartmentRanking(userId);
        } else {
            // 특정 날짜의 확정된 주간 랭킹
            response =  departmentRankService.getCompletedWeeklyDepartmentRanking(userId, date);
        }

        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
    /*
    학과 랭킹 월간 조회
     */
    @GetMapping("/monthly")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<DepartmentRankingResponse>> getMonthlyRanking(Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date){
        DepartmentRankingResponse response;

        if (date == null) {
            // 현재 진행중인 월간 랭킹
            response = departmentRankService.getCurrentMonthlyDepartmentRanking(userId);
        } else {
            // 특정 날짜의 확정된 월간 랭킹
            response =  departmentRankService.getCompletedMonthlyDepartmentRanking(userId, date);
        }

        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
