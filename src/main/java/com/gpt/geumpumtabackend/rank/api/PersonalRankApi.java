package com.gpt.geumpumtabackend.rank.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.rank.dto.response.PersonalRankingResponse;
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

@Tag(name = "개인 랭킹 API", description = """
    사용자들의 개인 학습 시간 랭킹을 제공합니다.
    
    📊 **랭킹 시스템 특징:**
    - 일간/주간/월간 랭킹 지원
    - 실시간 진행 중 랭킹과 확정된 과거 랭킹 분리
    - 현재 사용자의 순위와 전체 상위 랭킹 제공
    
    🔄 **랭킹 업데이트:**
    - 실시간: 학습 세션 종료 시마다 반영
    - 확정: 매일 새벽 3시 자동 계산
    """)
public interface PersonalRankApi {

    @Operation(
            summary = "일간 개인 랭킹 조회",
            description = """
            일간 개인 학습 시간 랭킹을 조회합니다. date로  오늘 날짜의 00:00을 보냅니다.
            
            📅 **조회 기간:**
            - date 파라미터 없음: 오늘 00:00 ~ 현재 (실시간)
            - date 파라미터 있음: 해당 날짜의 확정된 랭킹
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = PersonalRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = PersonalRankingResponse.class,
                    description = "일간 개인 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/daily")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<PersonalRankingResponse>> getDailyRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "특정 날짜 (생략시 오늘 실시간 랭킹)",
                    example = "2024-03-15T00:00:00"
            )
            @RequestParam(required = false) LocalDateTime date
    );

    @Operation(
            summary = "주간 개인 랭킹 조회", 
            description = """
            주간 개인 학습 시간 랭킹을 조회합니다. date로 각 주의 월요일 00:00을 보냅니다.
            
            📅 **조회 기간:**
            - date 파라미터 없음: 이번 주 월요일 00:00 ~ 현재 (실시간)
            - date 파라미터 있음: 해당 주의 확정된 랭킹
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = PersonalRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = PersonalRankingResponse.class,
                    description = "주간 개인 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/weekly")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<PersonalRankingResponse>> getWeeklyRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "특정 주의 날짜 (생략시 이번 주 실시간 랭킹)",
                    example = "2024-03-11T00:00:00"
            )
            @RequestParam(required = false) java.time.LocalDateTime date
    );

    @Operation(
            summary = "월간 개인 랭킹 조회",
            description = """
            월간 개인 학습 시간 랭킹을 조회합니다. date로 각 월의 1일  00:00을 보냅니다.
            
            📅 **조회 기간:**
            - date 파라미터 없음: 이번 달 1일 00:00 ~ 현재 (실시간)
            - date 파라미터 있음: 해당 월의 확정된 랭킹
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = PersonalRankingResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = PersonalRankingResponse.class,
                    description = "월간 개인 랭킹 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/monthly")
    @AssignUserId  
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<PersonalRankingResponse>> getMonthlyRanking(
            @Parameter(hidden = true) Long userId,
            @Parameter(
                    description = "특정 월의 날짜 (생략시 이번 달 실시간 랭킹)",
                    example = "2024-03-01T00:00:00"
            )
            @RequestParam(required = false) java.time.LocalDateTime date
    );
}