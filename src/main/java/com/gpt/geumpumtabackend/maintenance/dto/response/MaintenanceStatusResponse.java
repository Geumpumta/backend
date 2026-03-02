package com.gpt.geumpumtabackend.maintenance.dto.response;

import com.gpt.geumpumtabackend.maintenance.domain.Maintenance;
import com.gpt.geumpumtabackend.maintenance.domain.ServiceStatus;

public record MaintenanceStatusResponse(
        ServiceStatus status,
        String message
) {
    public static MaintenanceStatusResponse from(Maintenance maintenance) {
        return new MaintenanceStatusResponse(
                maintenance.getStatus(),
                maintenance.getMessage()
        );
    }
}
