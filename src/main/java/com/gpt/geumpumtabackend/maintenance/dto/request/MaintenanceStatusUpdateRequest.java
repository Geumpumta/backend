package com.gpt.geumpumtabackend.maintenance.dto.request;

import com.gpt.geumpumtabackend.maintenance.domain.ServiceStatus;
import jakarta.validation.constraints.NotNull;

public record MaintenanceStatusUpdateRequest(
        @NotNull ServiceStatus status,
        String message
) {
}
