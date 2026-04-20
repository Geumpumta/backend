package com.gpt.geumpumtabackend.badge.controller;

import com.gpt.geumpumtabackend.badge.api.BadgeApi;
import com.gpt.geumpumtabackend.badge.dto.request.BadgeCreateRequest;
import com.gpt.geumpumtabackend.badge.dto.request.RepresentativeBadgeRequest;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeCreateResponse;
import com.gpt.geumpumtabackend.badge.dto.response.BadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeResponse;
import com.gpt.geumpumtabackend.badge.dto.response.MyBadgeStatusResponse;
import com.gpt.geumpumtabackend.badge.dto.response.RepresentativeBadgeResponse;
import com.gpt.geumpumtabackend.badge.service.BadgeService;
import com.gpt.geumpumtabackend.global.aop.AssignUserId;
import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/badge")
public class BadgeController implements BadgeApi {

    private final BadgeService badgeService;

    @PostMapping
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<BadgeCreateResponse>> createBadge(
            @RequestBody @Valid BadgeCreateRequest request
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                badgeService.createBadge(request)
        ));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<List<BadgeResponse>>> getAllBadges() {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                badgeService.getAllBadges()
        ));
    }

    @DeleteMapping("/{badgeId}")
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<Void>> deleteBadge(
            @PathVariable Long badgeId
    ) {
        badgeService.deleteBadge(badgeId);
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse());
    }

    @GetMapping("/me")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<List<MyBadgeStatusResponse>>> getMyBadges(
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                badgeService.getMyBadges(userId)
        ));
    }

    @PostMapping("/me/representative-badge")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<RepresentativeBadgeResponse>> setRepresentativeBadge(
            @RequestBody RepresentativeBadgeRequest request,
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                badgeService.setRepresentativeBadge(request, userId)
        ));
    }

    @GetMapping("/unnotified")
    @AssignUserId
    @PreAuthorize("isAuthenticated() and hasRole('USER')")
    public ResponseEntity<ResponseBody<List<MyBadgeResponse>>> getUnnotifiedBadges(
            Long userId
    ){
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                badgeService.getUnnotifiedBadges(userId)
        ));
    }
}
