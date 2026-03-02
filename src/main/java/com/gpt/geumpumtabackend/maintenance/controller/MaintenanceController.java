package com.gpt.geumpumtabackend.maintenance.controller;

import com.gpt.geumpumtabackend.global.response.ResponseBody;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.maintenance.dto.request.MaintenanceStatusUpdateRequest;
import com.gpt.geumpumtabackend.maintenance.dto.response.MaintenanceStatusResponse;
import com.gpt.geumpumtabackend.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping("/status")
    public ResponseEntity<ResponseBody<MaintenanceStatusResponse>> getCurrentStatus() {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                maintenanceService.getCurrentStatus()
        ));
    }

    @PatchMapping("/status")
    @PreAuthorize("isAuthenticated() and hasRole('ADMIN')")
    public ResponseEntity<ResponseBody<MaintenanceStatusResponse>> updateStatus(
            @RequestBody @Valid MaintenanceStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ResponseUtil.createSuccessResponse(
                maintenanceService.updateStatus(request)
        ));
    }
}
