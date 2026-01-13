package com.gpt.geumpumtabackend.image.api;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiFailedResponse;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiResponses;
import com.gpt.geumpumtabackend.global.config.swagger.SwaggerApiSuccessResponse;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.image.dto.request.ImageUploadRequest;
import com.gpt.geumpumtabackend.image.dto.response.ImageUploadSuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

public interface ImageApi {

    @Operation(
            summary =  "이미지 업로드를 위한 api",
            description = "이미지를 클라우드에 업로드하고 이미지 url과 이미지 식별자 public_id를 받습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ImageUploadRequest.class)
                    )
            )
    )
    @ApiResponse(content = @Content(schema = @Schema(implementation = ImageUploadSuccessResponse.class)))
    @SwaggerApiResponses(
            success = @SwaggerApiSuccessResponse(
                    response = ImageUploadSuccessResponse.class,
                    description = "이미지 업로드 완료"),
            errors = {
                    @SwaggerApiFailedResponse(ExceptionType.NEED_AUTHORIZED),
                    @SwaggerApiFailedResponse(ExceptionType.USER_NOT_FOUND),
            }
    )
    @PostMapping("/profile")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<ImageUploadSuccessResponse>> uploadProfileImage(
            @RequestParam MultipartFile image,
            @Parameter(hidden = true) Long userId
    );
}
