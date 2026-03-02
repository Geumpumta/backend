package com.gpt.geumpumtabackend.unit.maintenance.service;

import com.gpt.geumpumtabackend.maintenance.domain.Maintenance;
import com.gpt.geumpumtabackend.maintenance.domain.ServiceStatus;
import com.gpt.geumpumtabackend.maintenance.dto.request.MaintenanceStatusUpdateRequest;
import com.gpt.geumpumtabackend.maintenance.dto.response.MaintenanceStatusResponse;
import com.gpt.geumpumtabackend.maintenance.repository.MaintenanceRepository;
import com.gpt.geumpumtabackend.maintenance.service.MaintenanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaintenanceService 단위 테스트")
class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    @DisplayName("점검 상태가 없으면 생성 후 저장한다")
    void 상태가_없으면_생성후_저장한다() {
        MaintenanceStatusUpdateRequest request = new MaintenanceStatusUpdateRequest(
                ServiceStatus.MAINTENANCE,
                "점검 중입니다."
        );
        Maintenance saved = Maintenance.initialize(ServiceStatus.MAINTENANCE, "점검 중입니다.");

        when(maintenanceRepository.findById(Maintenance.DEFAULT_ID)).thenReturn(Optional.empty());
        when(maintenanceRepository.save(any(Maintenance.class))).thenReturn(saved);

        MaintenanceStatusResponse response = maintenanceService.updateStatus(request);

        assertThat(response.status()).isEqualTo(ServiceStatus.MAINTENANCE);
        assertThat(response.message()).isEqualTo("점검 중입니다.");
        verify(maintenanceRepository).save(any(Maintenance.class));
    }

    @Test
    @DisplayName("기존 상태가 있으면 값을 갱신한다")
    void 기존_상태가_있으면_갱신한다() {
        Maintenance maintenance = Maintenance.initialize(ServiceStatus.NORMAL, null);
        MaintenanceStatusUpdateRequest request = new MaintenanceStatusUpdateRequest(
                ServiceStatus.NORMAL,
                "정상 운영 중입니다."
        );

        when(maintenanceRepository.findById(Maintenance.DEFAULT_ID)).thenReturn(Optional.of(maintenance));
        when(maintenanceRepository.save(maintenance)).thenReturn(maintenance);

        MaintenanceStatusResponse response = maintenanceService.updateStatus(request);

        assertThat(response.status()).isEqualTo(ServiceStatus.NORMAL);
        assertThat(response.message()).isEqualTo("정상 운영 중입니다.");
        verify(maintenanceRepository).save(maintenance);
    }
}
