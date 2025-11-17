package com.gpt.geumpumtabackend.image.controller;

import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.image.api.ImageApi;
import com.gpt.geumpumtabackend.image.dto.response.ImageUploadSuccessResponse;
import com.gpt.geumpumtabackend.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/image")
@RequiredArgsConstructor
public class ImageController implements ImageApi {

    private final ImageService imageService;

    // TODO : 새로운 이미지 업로드 시 이전 이미지 삭제하는 기능 필요
    @PostMapping("/profile")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<ImageUploadSuccessResponse>> uploadProfileImage(
            @RequestParam MultipartFile image,
            Long userId
    ) {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                imageService.uploadImage(image, "profile/" + userId + "/")
        ));
    }
}
