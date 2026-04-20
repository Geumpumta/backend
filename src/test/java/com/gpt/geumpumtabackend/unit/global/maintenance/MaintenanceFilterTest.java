package com.gpt.geumpumtabackend.unit.global.maintenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.global.maintenance.MaintenanceFilter;
import com.gpt.geumpumtabackend.maintenance.service.MaintenanceService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaintenanceFilter 단위 테스트")
class MaintenanceFilterTest {

    @Mock
    private MaintenanceService maintenanceService;

    private MaintenanceFilter maintenanceFilter;

    @BeforeEach
    void setUp() {
        maintenanceFilter = new MaintenanceFilter(maintenanceService, new ObjectMapper());
    }

    @Test
    @DisplayName("점검 중이면 일반 API 요청을 503으로 차단한다")
    void 점검중이면_일반요청을_차단한다() throws Exception {
        when(maintenanceService.isMaintenanceInProgress()).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/board/list");
        request.setServletPath("/api/v1/board/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);

        maintenanceFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("MT001");
        assertThat(response.getContentAsString()).contains("서버 점검 중입니다.");
        assertThat(chainCalled).isFalse();
    }

    @Test
    @DisplayName("점검 중이어도 화이트리스트 경로는 통과시킨다")
    void 점검중이어도_화이트리스트는_통과한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/maintenance/status");
        request.setServletPath("/api/v1/maintenance/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);

        maintenanceFilter.doFilter(request, response, filterChain);

        assertThat(chainCalled).isTrue();
        verifyNoInteractions(maintenanceService);
    }

    @Test
    @DisplayName("컨텍스트 패스가 있어도 화이트리스트 경로는 통과시킨다")
    void 컨텍스트패스가_있어도_화이트리스트는_통과한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/geumpumta/api/v1/maintenance/status");
        request.setContextPath("/geumpumta");
        request.setServletPath("/api/v1/maintenance/status");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);

        maintenanceFilter.doFilter(request, response, filterChain);

        assertThat(chainCalled).isTrue();
        verifyNoInteractions(maintenanceService);
    }

    @Test
    @DisplayName("정상 상태면 일반 API 요청을 통과시킨다")
    void 정상상태면_일반요청을_통과시킨다() throws Exception {
        when(maintenanceService.isMaintenanceInProgress()).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/board/list");
        request.setServletPath("/api/v1/board/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> chainCalled.set(true);

        maintenanceFilter.doFilter(request, response, filterChain);

        assertThat(chainCalled).isTrue();
        assertThat(response.getContentAsString()).isEmpty();
    }
}
