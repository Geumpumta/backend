package com.gpt.geumpumtabackend.study.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.study.dto.request.HeartBeatRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyEndRequest;
import com.gpt.geumpumtabackend.study.dto.request.StudyStartRequest;
import com.gpt.geumpumtabackend.study.dto.response.StudySessionResponse;
import com.gpt.geumpumtabackend.study.dto.response.StudyStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "학습 세션 API", description = """
    금오공대 캠퍼스 내에서만 사용할 수 있는 학습 타이머 기능을 제공합니다.
    

    📋 **사용 흐름:**
    1. `GET /api/v1/study` - 오늘 총 학습 시간 조회
    2. `POST /api/v1/study/start` - 학습 시작 (Wi-Fi 검증 필수)
    3. `POST /api/v1/study/heart-beat` - 30초마다 연결 상태 확인
    4. `POST /api/v1/study/end` - 학습 종료
    
    ⚠️ **중요사항:**
    - 모든 API는 캠퍼스 Wi-Fi 연결 시에만 작동
    - 하트비트 중단시 90초 후 자동 세션 종료
    """)
public interface StudySessionApi {

    @Operation(
            summary = "오늘의 학습 시간 조회",
            description = """
            사용자의 오늘 하루 총 학습 시간을 조회합니다.
            
            📊 **반환 정보:**
            - 오늘 00:00부터 현재까지의 누적 학습 시간 (밀리초)
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = StudySessionResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = StudySessionResponse.class,
                    description = "오늘의 학습 시간 조회 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @GetMapping
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<StudySessionResponse>> getTodayStudySession(
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "학습 세션 시작",
            description = """
            새로운 학습 세션을 시작합니다. 캠퍼스 Wi-Fi 검증이 필수입니다.
            
            🔐 **Wi-Fi 검증 과정:**
            1. gatewayIp 검증 - 허용된 캠퍼스 Wi-Fi인지 확인
            2. BSSID 검증 - 특정 액세스포인트 확인 (선택사항)
            3. IP 대역 검증 - 캠퍼스 IP 범위 내인지 확인
            
            ✅ **성공 시:**
            - 새로운 학습 세션 생성
            - 세션 ID 반환 (하트비트에서 사용)
            
            ❌ **실패 사유:**
            - 캠퍼스 외부에서 접근
            - Wi-Fi 정보 불일치
            """
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = StudyStartResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = StudyStartResponse.class,
                    description = "학습 세션 시작 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.WIFI_NOT_CAMPUS_NETWORK),
                    @SwaggerApiFailedResponse(ExceptionType.WIFI_VALIDATION_ERROR),
                    @SwaggerApiFailedResponse(ExceptionType.WIFI_INVALID_FORMAT)
            }
    )
    @PostMapping("/start")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<StudyStartResponse>> startStudySession(
            @Valid @RequestBody StudyStartRequest request,
            @Parameter(hidden = true) Long userId,
            HttpServletRequest httpServletRequest
    );

    @Operation(
            summary = "학습 세션 종료",
            description = """
            현재 진행 중인 학습 세션을 종료합니다.
            
            📊 **종료 시 처리:**
            - 총 학습 시간 계산 및 저장
            - 세션 상태를 FINISHED로 변경
            - 랭킹 시스템에 반영 (다음 스케줄링 시)
            """
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    description = "학습 세션 종료 성공"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.STUDY_SESSION_NOT_FOUND)
            }
    )
    @PostMapping("/end")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<Void>> endStudySession(
            @Valid @RequestBody StudyEndRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "하트비트 전송",
            description = """
            학습 중 연결 상태를 유지하기 위한 하트비트를 전송합니다.
            
            ⏱️ **전송 주기:** 30초마다 자동 전송 권장
            
            🔄 **동작 원리:**
            1. Wi-Fi 연결 상태 재검증
            2. 세션의 lastHeartBeatAt 시간 업데이트
            3. 90초 이상 하트비트 없으면 좀비 세션으로 분류
            
            🚨 **실패 시 대응:**
            - Wi-Fi 연결 끊김: 재연결 후 다시 `/start` 호출
            - 세션 만료: 새로운 세션 시작 필요
            
            """
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    description = "하트비트 전송 성공 - 세션 유지"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.STUDY_SESSION_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.WIFI_NOT_CAMPUS_NETWORK),
                    @SwaggerApiFailedResponse(ExceptionType.WIFI_VALIDATION_ERROR)
            }
    )
    @PostMapping("/heart-beat")
    @AssignUserId  
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<Void>> processHeartBeat(
            @Valid @RequestBody HeartBeatRequest heartBeatRequest,
            @Parameter(hidden = true) Long userId,
            HttpServletRequest httpServletRequest);
}