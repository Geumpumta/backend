package com.gpt.geumpumtabackend.global.maintenance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpt.geumpumtabackend.global.exception.ExceptionType;
import com.gpt.geumpumtabackend.global.response.ResponseUtil;
import com.gpt.geumpumtabackend.maintenance.service.MaintenanceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST = List.of(
            "/api/v1/maintenance/status",
            "/actuator/health",
            "/swagger-ui",
            "/swagger-ui/",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/error"
    );

    private final MaintenanceService maintenanceService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestUri = request.getServletPath();

        if (isWhitelisted(requestUri) || !maintenanceService.isMaintenanceInProgress()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(ExceptionType.MAINTENANCE_IN_PROGRESS.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ResponseUtil.createFailureResponse(ExceptionType.MAINTENANCE_IN_PROGRESS)
        );
    }

    private boolean isWhitelisted(String requestUri) {
        return WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }
}
