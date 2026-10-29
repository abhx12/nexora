package com.nexora.reporting.api.dto;

import java.math.BigDecimal;
import java.util.List;

// Post-MVP AI Financial Advisory & Forecasting (01-PRD, ROADMAP).
public record AiAdvisoryResponse(
        HealthStatus healthStatus,
        BigDecimal liquidityCoverageRatio,
        Integer cashRunwayDays,
        Integer silkRunwayDays,
        List<AdvisoryInsight> insights) {

    public enum HealthStatus {
        HEALTHY,
        ATTENTION_NEEDED,
        CRITICAL
    }

    public enum InsightType {
        LIQUIDITY,
        RECEIVABLES,
        INVENTORY,
        PROFITABILITY
    }

    public enum Severity {
        SUCCESS,
        INFO,
        WARNING,
        CRITICAL
    }

    public record AdvisoryInsight(
            InsightType type,
            Severity severity,
            String titleEn,
            String titleTa,
            String detailEn,
            String detailTa,
            String actionableStepEn,
            String actionableStepTa) {
    }
}
