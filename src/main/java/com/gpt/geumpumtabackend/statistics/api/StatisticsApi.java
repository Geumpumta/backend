package com.gpt.geumpumtabackend.statistics.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.statistics.dto.response.DailyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.GrassStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.MonthlyStatisticsResponse;
import com.gpt.geumpumtabackend.statistics.dto.response.WeeklyStatisticsResponse;
import com.gpt.geumpumtabackend.token.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "통계 API", description = "통계 관련 API")
public interface StatisticsApi {

    @Operation(
            summary =  "일간 통계 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 일간 통계를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = DailyStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = DailyStatisticsResponse.class,
                    description = "일간 통계 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/day")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<DailyStatisticsResponse>> getMyDailyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "주간 통계 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 주간 통계를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = WeeklyStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = WeeklyStatisticsResponse.class,
                    description = "주간 통계 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/week")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<WeeklyStatisticsResponse>> getMyWeeklyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "월간 통계 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 월간 통계를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = MonthlyStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = MonthlyStatisticsResponse.class,
                    description = "월간 통계 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/month")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<MonthlyStatisticsResponse>> getMyMonthlyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "잔디 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 잔디를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = GrassStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = GrassStatisticsResponse.class,
                    description = "잔디 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/grass")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<GrassStatisticsResponse>> getMyGrassStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary =  "다른 사용자의 일간 통계 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 다른 사용자의 일간 통계를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = DailyStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = DailyStatisticsResponse.class,
                    description = "일간 통계 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/day")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<DailyStatisticsResponse>> getDailyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long targetUserId,
            Long userId
    );

    @Operation(
            summary =  "다른 사용자의 주간 통계 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 다른 사용자의 주간 통계를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = WeeklyStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = WeeklyStatisticsResponse.class,
                    description = "주간 통계 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/week")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<WeeklyStatisticsResponse>> getWeeklyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long targetUserId,
            Long userId
    );

    @Operation(
            summary =  "다른 사용자의 월간 통계 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 다른 사용자의 월간 통계를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = MonthlyStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = MonthlyStatisticsResponse.class,
                    description = "월간 통계 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/month")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<MonthlyStatisticsResponse>> getMonthlyStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long targetUserId,
            Long userId
    );

    @Operation(
            summary =  "다른 사용자의 잔디 요청 api",
            description = "USER 이상의 권한을 가진 사용자는 다른 사용자의 잔디를 요청합니다."

    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = GrassStatisticsResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = GrassStatisticsResponse.class,
                    description = "잔디 요청 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @GetMapping("/grass")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<GrassStatisticsResponse>> getGrassStatistics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long targetUserId,
            Long userId
    );
}
