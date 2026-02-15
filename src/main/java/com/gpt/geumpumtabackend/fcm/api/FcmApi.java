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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "FCM API", description = """
    Firebase Cloud Messaging 알림 관련 API


    ## 서버 → 클라이언트 푸시 알림

    서버가 자동으로 전송하는 FCM 메시지 목록입니다. 클라이언트에서 수신 처리가 필요합니다.

    ### 최대 집중 시간 도달 알림

    3시간 연속 공부 시 서버가 세션을 자동 종료하고 아래 FCM 메시지를 전송합니다.

    **Notification (백그라운드 알림창 표시용):**
    ```json
    {
      "title": "최대 집중 시간 도달",
      "body": "3시간 동안 열심히 공부하셨습니다! 잠시 휴식을 취해보세요."
    }
    ```

    **Data Message (포그라운드 앱 핸들러용):**
    ```json
    {
      "type": "STUDY_SESSION_FORCE_ENDED",
      "maxFocusHours": "3"
    }
    ```

    **클라이언트 처리 가이드:**
    - **포그라운드**: `data.type == "STUDY_SESSION_FORCE_ENDED"` 수신 시 즉시 타이머 UI를 중지하고 종료 안내를 표시합니다.
    - **백그라운드**: `notification`이 알림창에 표시됩니다. 사용자가 앱에 복귀하면 `GET /api/v1/study`를 호출하여 세션 상태(`isStudying`)를 확인합니다.
    - **FCM 수신 실패 대비**: 앱이 포그라운드로 전환될 때마다 `GET /api/v1/study`를 호출하여 `isStudying=false`이면 타이머를 중지하는 폴백 로직을 구현해야 합니다.
    """)
public interface FcmApi {

    @Operation(
            summary = "FCM 토큰 등록",
            description = "사용자의 FCM 디바이스 토큰을 등록하여 푸시 알림을 받을 수 있도록 합니다."
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
    @PostMapping("/register")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    ResponseEntity<ResponseBody<Void>> registerFcmToken(
            @RequestBody @Valid FcmTokenRequest request,
            @Parameter(hidden = true) Long userId
    );

    @Operation(
            summary = "FCM 토큰 삭제",
            description = "등록된 FCM 토큰을 삭제합니다. 로그아웃 시 호출하여 알림을 받지 않도록 합니다."
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
            @Parameter(hidden = true) Long userId
    );
}
