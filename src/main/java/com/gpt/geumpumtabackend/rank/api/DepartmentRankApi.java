package com.gpt.geumpumtabackend.rank.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.rank.dto.response.DepartmentRankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Tag(name = "학과별 랭킹 API", description = """
    학과별 학습 시간 랭킹을 제공합니다.
    """)
public interface DepartmentRankApi {

    @Operation(
            summary = "일간 학과 랭킹 조회",
            description = """
            일간 학과별 학습 시간 랭킹을 조회합니다. 
            
            📅 **조회 기간:**
            - date : 오늘 날짜의 00:00
            - date 파라미터 없음: 오늘 00:00 ~ 현재 (실시간)
            - date 파라미터 있음: 해당 날짜의 확정된 랭킹
            
            🏆 **랭킹 정보:**
            - 현재 사용자 학과의 순위와 학습 시간
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = DepartmentRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = DepartmentRankingResponse.class,
                    description = "일간 학과 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/daily")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<DepartmentRankingResponse>> getDailyRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "특정 날짜 (생략시 오늘 실시간 랭킹)",
                    example = "2024-03-15T00:00:00"
            )
            @RequestParam(required = false) LocalDateTime date
    );

    @Operation(
            summary = "주간 학과 랭킹 조회",
            description = """
            주간 학과별 학습 시간 랭킹을 조회합니다. date로 각 주의 월요일 00:00을 보냅니다.
            
            📅 **조회 기간:**
            - date 파라미터 없음: 이번 주 월요일 00:00 ~ 현재 (실시간)
            - date 파라미터 있음: 해당 주의 확정된 랭킹
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = DepartmentRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = DepartmentRankingResponse.class,
                    description = "주간 학과 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/weekly")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<DepartmentRankingResponse>> getWeeklyRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "특정 주의 날짜 (생략시 이번 주 실시간 랭킹)",
                    example = "2024-03-11T00:00:00"
            )
            @RequestParam(required = false) LocalDateTime date
    );

    @Operation(
            summary = "월간 학과 랭킹 조회",
            description = """
            월간 학과별 학습 시간 랭킹을 조회합니다. date로 해당 월의 1일 00:00을 보냅니다.
            
            📅 **조회 기간:**
            - date 파라미터 없음: 이번 달 1일 00:00 ~ 현재 (실시간)
            - date 파라미터 있음: 해당 월의 확정된 랭킹
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = DepartmentRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = DepartmentRankingResponse.class,
                    description = "월간 학과 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/monthly")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<DepartmentRankingResponse>> getMonthlyRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "특정 월의 날짜 (생략시 이번 달 실시간 랭킹)",
                    example = "2024-03-01T00:00:00"
            )
            @RequestParam(required = false) java.time.LocalDateTime date
    );
}