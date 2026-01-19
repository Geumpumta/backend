package com.gpt.geumpumtabackend.rank.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.rank.dto.response.SeasonRankingResponse;
import com.gpt.geumpumtabackend.user.domain.Department;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "시즌 랭킹 API", description = """
    학기별 시즌 랭킹을 제공합니다.

    📅 **시즌 구성:**
    - 봄학기: 3월 1일 ~ 6월 30일
    - 여름방학: 7월 1일 ~ 8월 31일
    - 가을학기: 9월 1일 ~ 12월 31일
    - 겨울방학: 1월 1일 ~ 2월 말일

    🏆 **시즌 랭킹 특징:**
    - 현재 활성 시즌: 실시간 랭킹 (월간+일간+오늘 누적)
    - 종료된 시즌: 스냅샷 기반 확정 랭킹
    - 전체 랭킹 및 학과별 랭킹 지원
    """)
public interface SeasonRankApi {

    @Operation(
            summary = "현재 시즌 전체 랭킹 조회",
            description = """
            현재 활성 중인 시즌의 전체 사용자 랭킹을 조회합니다.

            📊 **랭킹 계산:**
            - 완료된 월간 랭킹 합산
            - 현재 진행 중인 월의 일간 랭킹 합산
            - 오늘 실시간 학습 세션 합산
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = SeasonRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = SeasonRankingResponse.class,
                    description = "현재 시즌 전체 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.SEASON_NOT_FOUND)
            }
    )
    @GetMapping("/current")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<SeasonRankingResponse>> getCurrentSeasonRanking(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "현재 시즌 학과별 랭킹 조회",
            description = """
            현재 활성 중인 시즌의 특정 학과 랭킹을 조회합니다.

            📊 **랭킹 계산:**
            - 해당 학과 학생들만 필터링
            - 완료된 월간 랭킹 합산
            - 현재 진행 중인 월의 일간 랭킹 합산
            - 오늘 실시간 학습 세션 합산
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = SeasonRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = SeasonRankingResponse.class,
                    description = "현재 시즌 학과별 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.SEASON_NOT_FOUND)
            }
    )
    @GetMapping("/current/department")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<SeasonRankingResponse>> getCurrentSeasonDepartmentRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "학과 이름",
                    example = "COMPUTER_ENGINEERING",
                    required = true
            )
            @RequestParam Department department
    );

    @Operation(
            summary = "종료된 시즌 전체 랭킹 조회",
            description = """
            종료된 시즌의 전체 사용자 최종 랭킹을 조회합니다.

            💾 **스냅샷 기반:**
            - 시즌 종료 시점에 생성된 확정 랭킹 스냅샷
            - 시즌 종료 후 변경되지 않는 영구 기록
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = SeasonRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = SeasonRankingResponse.class,
                    description = "종료된 시즌 전체 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.SEASON_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.SEASON_NOT_ENDED)
            }
    )
    @GetMapping("/{seasonId}")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<SeasonRankingResponse>> getEndedSeasonRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "시즌 ID",
                    example = "1"
            )
            @PathVariable Long seasonId
    );

    @Operation(
            summary = "종료된 시즌 학과별 랭킹 조회",
            description = """
            종료된 시즌의 특정 학과 최종 랭킹을 조회합니다.

            💾 **스냅샷 기반:**
            - 시즌 종료 시점에 생성된 학과별 확정 랭킹 스냅샷
            - 시즌 종료 후 변경되지 않는 영구 기록
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = SeasonRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = SeasonRankingResponse.class,
                    description = "종료된 시즌 학과별 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.SEASON_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.SEASON_NOT_ENDED)
            }
    )
    @GetMapping("/{seasonId}/department")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<SeasonRankingResponse>> getEndedSeasonDepartmentRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "시즌 ID",
                    example = "1"
            )
            @PathVariable Long seasonId,
            @Parameter(
                    description = "학과 이름",
                    example = "COMPUTER_ENGINEERING",
                    required = true
            )
            @RequestParam Department department
    );
}
