package com.gpt.geumpumtabackend.badge.api;

import com.gpt.geumpumtabackend.badge.dto.request.BadgeCreateRequest;
import com.gpt.geumpumtabackend.badge.dto.request.RepresentativeBadgeRequest;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeCreateResponse;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeStatusResponse;
import com.gpt.geumpumtabackend.badge.dto.response.RepresentativeBadgeResponse;
import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "배지 API", description = """
    배지 생성/조회/삭제 및 사용자 배지 조회 기능을 제공합니다.
    """)
public interface BadgeApi {

    @Operation(
            summary = "배지 생성",
            description = """
                    ADMIN 권한으로 새로운 배지를 생성합니다.
                    - code: 배지 고유 코드 (중복 불가)
                    - badgeType: 배지 종류
                    - thresholdValue: 누적 시간/연속 일수 계열 배지 기준값
                    - rank: 시즌 랭킹 배지 등수 값(예: 1,2,3)
                    """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = BadgeCreateResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = BadgeCreateResponse.class,
                    description = "배지 생성 성공"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.ACCESS_DENIED),
                    @SwaggerApiFailedResponse(ExceptionType.BADGE_CODE_ALREADY_EXISTS)
            }
    )
    @PostMapping
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    ResponseEntity<ResponseBody<BadgeCreateResponse>> createBadge(
            @RequestBody @Valid BadgeCreateRequest request
    );

    @Operation(
            summary = "전체 배지 조회",
            description = "ADMIN 권한으로 전체 배지 목록을 조회합니다."
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = BadgeResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = BadgeResponse.class,
                    description = "전체 배지 조회 성공"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.ACCESS_DENIED)
            }
    )
    @GetMapping
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    ResponseEntity<ResponseBody<List<BadgeResponse>>> getAllBadges();

    @Operation(
            summary = "배지 삭제",
            description = """
                    ADMIN 권한으로 배지를 삭제합니다.
                    이미 사용자에게 지급된 이력이 있으면 삭제할 수 없고 B004(BADGE_IN_USE)를 반환합니다.
                    """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = ResponseBody.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(description = "배지 삭제 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.ACCESS_DENIED),
                    @SwaggerApiFailedResponse(ExceptionType.BADGE_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.BADGE_IN_USE)
            }
    )
    @DeleteMapping("/{badgeId}")
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    ResponseEntity<ResponseBody<Void>> deleteBadge(
            @PathVariable Long badgeId
    );

    @Operation(
            summary = "내 배지 조회",
            description = """
                    항상 전체 배지 목록을 반환합니다.
                    각 원소는 아래 정보를 포함합니다.
                    - owned: 사용자의 배지 보유 여부
                    - awardedAt: 배지 획득 시각 (owned=false이면 null)
                    """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = MyBadgeStatusResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = MyBadgeStatusResponse.class,
                    description = "내 배지 조회 성공"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/me")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<List<MyBadgeStatusResponse>>> getMyBadges(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "대표 배지 설정",
            description = """
                    보유한 배지 중 하나를 대표 배지로 설정합니다.
                    요청은 badgeCode 기준으로 처리됩니다.
                    """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = RepresentativeBadgeResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = RepresentativeBadgeResponse.class,
                    description = "대표 배지 설정 성공"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.BADGE_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.BADGE_NOT_OWNED)
            }
    )
    @PostMapping("/me/representative-badge")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<RepresentativeBadgeResponse>> setRepresentativeBadge(
            @RequestBody RepresentativeBadgeRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "미확인 배지 조회",
            description = """
                    사용자의 미확인 배지 목록을 조회합니다.
                    조회된 배지는 같은 요청 트랜잭션에서 확인 처리(notifiedAt 설정)됩니다.
                    """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = MyBadgeResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = MyBadgeResponse.class,
                    description = "미확인 배지 조회 성공"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping("/unnotified")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<List<MyBadgeResponse>>> getUnnotifiedBadges(
            @Parameter(hidden = true) Long userId
    );
}
