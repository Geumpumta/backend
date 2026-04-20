package com.gpt.geumpumtabackend.maintenance.service;

import com.gpt.geumpumtabackend.maintenance.domain.Maintenance;
import com.gpt.geumpumtabackend.maintenance.domain.ServiceStatus;
import com.gpt.geumpumtabackend.maintenance.dto.request.MaintenanceStatusUpdateRequest;
import com.gpt.geumpumtabackend.maintenance.dto.response.MaintenanceStatusResponse;
import com.gpt.geumpumtabackend.maintenance.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;

    @Transactional
    public MaintenanceStatusResponse updateStatus(MaintenanceStatusUpdateRequest request) {
        Maintenance maintenance = maintenanceRepository.findById(Maintenance.DEFAULT_ID)
                .orElseGet(() -> Maintenance.initialize(request.status(), request.message()));

        maintenance.update(request.status(), request.message());

        return MaintenanceStatusResponse.from(maintenanceRepository.save(maintenance));
    }

    public MaintenanceStatusResponse getCurrentStatus() {
        Maintenance maintenance = maintenanceRepository.findById(Maintenance.DEFAULT_ID)
                .orElseGet(() -> Maintenance.initialize(
                        ServiceStatus.NORMAL,
                        null
                ));
        return MaintenanceStatusResponse.from(maintenance);
    }

    public boolean isMaintenanceInProgress() {
        return getCurrentStatus().status() == ServiceStatus.MAINTENANCE;
    }
}
