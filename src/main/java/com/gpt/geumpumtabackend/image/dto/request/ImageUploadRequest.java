package com.gpt.geumpumtabackend.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public record ImageUploadRequest(
        @Schema(type = "string", format = "binary", description = "이미지 파일")
        MultipartFile image
) {
}
