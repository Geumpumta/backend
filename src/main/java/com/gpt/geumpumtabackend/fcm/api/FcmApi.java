package com.gpt.geumpumtabackend.fcm.api;

import com.gpt.geumpumtabackend.fcm.dto.request.FcmTokenRequest;
import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "FCM API", description = """
    Firebase Cloud Messaging 푸시 알림 API

    ## 클라이언트 호출 가이드 (권장)

    1. 앱 시작 또는 로그인 직후
       - FCM 권한 허용 후 디바이스 토큰 발급 시 `POST /api/v1/fcm/token` 호출
    2. 토큰 갱신 이벤트(OnNewToken)
       - 토큰이 변경되면 즉시 `POST /api/v1/fcm/token` 재호출
    3. 로그아웃/알림 비활성화
       - `DELETE /api/v1/fcm/token` 호출하여 서버 토큰 바인딩 정리
    4. 계정 탈퇴
       - 즉시 삭제 요청을 보내고, 서버 정리 동작도 함께 기대

    ## 자동으로 전송되는 FCM 메시지

    ### 최대 공부시간 초과 알림
    3시간 연속 공부 시 서버가 세션을 자동 종료하고 아래 FCM 메시지를 전송합니다.

    **Notification (클라이언트 화면 표시):**
    ```json
    {
      "title": "최대 공부시간 초과",
      "body": "3시간 이상 연속 공부가 제한되어 세션이 자동 종료됩니다."
    }
    ```

    **Data Message (앱 내부 처리):**
    ```json
    {
      "type": "STUDY_SESSION_FORCE_ENDED",
      "maxFocusHours": "3"
    }
    ```

    **클라이언트 처리 가이드:**
    - data.type == "STUDY_SESSION_FORCE_ENDED" 이면 세션 강제 종료 UI로 이동
    - notification은 화면 표시용으로 활용

    ## FCM 전송 오류 처리 정책

    서버는 FCM 전송 실패 시 오류 유형에 따라 다르게 처리합니다.

    ### 영구 오류 (재시도 없음)
    | ErrorCode | 처리 |
    |-----------|------|
    | `UNREGISTERED` | 무효 토큰 자동 삭제 후 전송 중단 (앱 삭제/재설치 시 발생) |
    | `INVALID_ARGUMENT` | 로그 경고 후 전송 중단 |
    | `SENDER_ID_MISMATCH` | 로그 경고 후 전송 중단 |
    | `THIRD_PARTY_AUTH_ERROR` | 로그 경고 후 전송 중단 |

    ### 일시적 오류 (최대 3회 재시도, 1s → 2s → 4s backoff)
    | ErrorCode | 설명 |
    |-----------|------|
    | `UNAVAILABLE` | FCM 서버 일시 장애 |
    | `INTERNAL` | FCM 내부 오류 |
    | `QUOTA_EXCEEDED` | 전송 한도 초과 |

    3회 재시도 실패 시 `F001 FCM_SEND_FAILED` 오류가 발생하지만, 최대 집중시간 알림의 경우 로그만 남기고 세션 종료에는 영향을 주지 않습니다.

    ### 클라이언트 권장 사항
    - `UNREGISTERED` 발생 시 서버가 토큰을 자동 삭제하므로, 앱 재시작 시 `POST /api/v1/fcm/token`을 다시 호출하세요.
    """)
public interface FcmApi {

    @Operation(
            summary = "FCM 토큰 등록",
            description = "요청자의 FCM 디바이스 토큰을 등록합니다. 앱 시작/로그인 후 또는 토큰 갱신 시 호출하세요."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = Void.class,
                    description = "FCM 토큰 등록 완료"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
                    @SwaggerApiFailedResponse(ExceptionType.FCM_INVALID_TOKEN)
            }
    )
    @PostMapping("/token")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<Void>> registerFcmToken(
            @RequestBody @Valid FcmTokenRequest request,
            @Parameter(hidden = true) Long userId,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "FCM 토큰 삭제",
            description = "등록된 FCM 토큰을 삭제합니다. 로그아웃, 알림 비활성화, 탈퇴 시 호출하세요."
    )
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = Void.class,
                    description = "FCM 토큰 삭제 완료"
            ),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND)
            }
    )
    @DeleteMapping("/token")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<Void>> removeFcmToken(
            @Parameter(hidden = true) Long userId,
            @Parameter(hidden = true) Authentication authentication
    );
}
