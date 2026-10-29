package com.nexora.reporting.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexora.reporting.api.dto.AiAdvisoryResponse;
import com.nexora.reporting.api.dto.DashboardResponse;
import com.nexora.reporting.application.AiAdvisoryService;
import com.nexora.reporting.application.DashboardQuery;

// The owner's one-screen view of money, obligations, stock and work (E-RPT-01).
@RestController
class DashboardController {

    private final DashboardQuery dashboard;
    private final AiAdvisoryService advisory;

    DashboardController(DashboardQuery dashboard, AiAdvisoryService advisory) {
        this.dashboard = dashboard;
        this.advisory = advisory;
    }

    @GetMapping("/api/v1/dashboard/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    DashboardResponse summary() {
        return dashboard.summary();
    }

    // Post-MVP AI Financial Advisory & Forecasting
    @GetMapping("/api/v1/dashboard/ai-advisory")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    AiAdvisoryResponse advisory() {
        return advisory.advise();
    }
}
