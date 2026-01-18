package com.gpt.geumpumtabackend.rank.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.rank.api.SeasonRankApi;
import com.gpt.geumpumtabackend.rank.dto.response.SeasonRankingResponse;
import com.gpt.geumpumtabackend.rank.service.SeasonRankService;
import com.gpt.geumpumtabackend.user.domain.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rank/season")
@RequiredArgsConstructor
public class SeasonRankController implements SeasonRankApi {

    private final SeasonRankService seasonRankService;

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<SeasonRankingResponse>> getCurrentSeasonRanking(Long userId) {
        SeasonRankingResponse response = seasonRankService.getCurrentSeasonRanking();
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/current/department")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<SeasonRankingResponse>> getCurrentSeasonDepartmentRanking(
            Long userId,
            @RequestParam Department department
    ) {
        SeasonRankingResponse response = seasonRankService.getCurrentSeasonDepartmentRanking(department);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/{seasonId}")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<SeasonRankingResponse>> getEndedSeasonRanking(
            Long userId,
            @PathVariable Long seasonId
    ) {
        SeasonRankingResponse response = seasonRankService.getEndedSeasonRanking(seasonId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }

    @GetMapping("/{seasonId}/department")
    @PreAuthorize("isAuthenticated() AND hasRole('USER')")
    @AssignUserId
    public ResponseEntity<ResponseBody<SeasonRankingResponse>> getEndedSeasonDepartmentRanking(
            Long userId,
            @PathVariable Long seasonId,
            @RequestParam Department department
    ) {
        SeasonRankingResponse response = seasonRankService.getEndedSeasonDepartmentRanking(seasonId, department);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(response));
    }
}
