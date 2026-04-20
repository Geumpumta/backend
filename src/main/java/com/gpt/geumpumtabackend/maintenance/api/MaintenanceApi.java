package com.gpt.geumpumtabackend.maintenance.api;

import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.maintenance.dto.request.MaintenanceStatusUpdateRequest;
import com.gpt.geumpumtabackend.maintenance.dto.response.MaintenanceStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "서비스 상태 API", description = "서비스 점검 상태 조회 및 변경 API")
public interface MaintenanceApi {

    @Operation(
            summary = "점검 상태 조회",
            description = "현재 서비스 점검 상태와 안내 메시지를 조회합니다."
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = MaintenanceStatusResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = MaintenanceStatusResponse.class,
                    description = "점검 상태 조회 성공"
            )
    )
    @GetMapping("/status")
    ResponseEntity<ResponseBody<MaintenanceStatusResponse>> getCurrentStatus();

    @Operation(
            summary = "점검 상태 변경",
            description = """
                    ADMIN 권한으로 서비스 점검 상태를 변경합니다.
                    - status: NORMAL 또는 MAINTENANCE
                    - message: 점검 안내 문구 (선택)
                    """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = MaintenanceStatusResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = MaintenanceStatusResponse.class,
                    description = "점검 상태 변경 성공"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.ACCESS_DENIED)
            }
    )
    @PatchMapping("/status")
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    ResponseEntity<ResponseBody<MaintenanceStatusResponse>> updateStatus(
            @RequestBody @Valid MaintenanceStatusUpdateRequest request
    );
}
