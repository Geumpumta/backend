package com.gpt.geumpumtabackend.rank.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
import com.gpt.geumpumtabackend.rank.service.PersonalRankService;
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
@RequestMapping("/api/v1/rank/personal")
@RequiredArgsConstructor
public class PersonalRankController {

    private final PersonalRankService personalRankService;

    /*
    개인 일간 랭킹 조회
     */
    @GetMapping("/daily")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<PersonalRankingResponse>> getDailyRanking(Long userId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date){
        PersonalRankingResponse response;

        if (date == null) {
            // 현재 진행중인 일간 랭킹
            response = personalRankService.getCurrentDaily(userId);
        } else {
            // 특정 날짜의 확정된 일간 랭킹
            response = personalRankService.getCompletedDaily(userId, date);
        }

        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
    /*
    개인 주간 랭킹 조회
     */
    @GetMapping("/weekly")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<PersonalRankingResponse>> getWeeklyRanking(Long userId, @RequestParam(required = false) LocalDateTime date){
        PersonalRankingResponse response;

        if (date == null) {
            // 현재 진행중인 주간 랭킹
            response = personalRankService.getCurrentWeekly(userId);
        } else {
            // 특정 날짜의 확정된 주간 랭킹
            response = personalRankService.getCompletedWeekly(userId, date);
        }

        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
    /*
    개인 월간 랭킹 조회
     */
    @GetMapping("/monthly")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<PersonalRankingResponse>> getMonthlyRanking(Long userId, @RequestParam(required = false) LocalDateTime date){
        PersonalRankingResponse response;

        if (date == null) {
            // 현재 진행중인 월간 랭킹
            response = personalRankService.getCurrentMonthly(userId);
        } else {
            // 특정 날짜의 확정된 월간 랭킹
            response = personalRankService.getCompletedMonthly(userId, date);
        }

        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
