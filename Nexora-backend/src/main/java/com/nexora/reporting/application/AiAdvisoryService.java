package com.nexora.reporting.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.nexora.procurement.api.dto.SupplierDueResponse;
import com.nexora.reporting.api.dto.AiAdvisoryResponse;
import com.nexora.reporting.api.dto.AiAdvisoryResponse.AdvisoryInsight;
import com.nexora.reporting.api.dto.AiAdvisoryResponse.HealthStatus;
import com.nexora.reporting.api.dto.AiAdvisoryResponse.InsightType;
import com.nexora.reporting.api.dto.AiAdvisoryResponse.Severity;
import com.nexora.reporting.api.dto.DashboardResponse;

// Post-MVP AI Financial Advisory & Forecasting service (01-PRD, ROADMAP).
// Synthesizes liquidity, order pipeline, inventory runway, and upcoming commitments into actionable guidance.
@Service
public class AiAdvisoryService {

    private final DashboardQuery dashboardQuery;

    public AiAdvisoryService(DashboardQuery dashboardQuery) {
        this.dashboardQuery = dashboardQuery;
    }

    public AiAdvisoryResponse advise() {
        DashboardResponse d = dashboardQuery.summary();
        List<AdvisoryInsight> insights = new ArrayList<>();

        // 1. Calculate immediate obligations (due within 7 days + overdue + worker + chit commitments)
        BigDecimal immediateDues = d.supplierOverdue();
        if (d.upcomingSupplierDues() != null) {
            for (SupplierDueResponse due : d.upcomingSupplierDues()) {
                if (due.outstanding() != null) {
                    immediateDues = immediateDues.add(due.outstanding());
                }
            }
        }
        if (d.workerPayable() != null) {
            immediateDues = immediateDues.add(d.workerPayable());
        }
        if (d.chitMonthlyCommitments() != null) {
            immediateDues = immediateDues.add(d.chitMonthlyCommitments());
        }

        BigDecimal available = d.totalAvailable() != null ? d.totalAvailable() : BigDecimal.ZERO;

        BigDecimal coverageRatio;
        if (immediateDues.compareTo(BigDecimal.ZERO) == 0) {
            coverageRatio = BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP);
        } else {
            coverageRatio = available.divide(immediateDues, 2, RoundingMode.HALF_UP);
        }

        HealthStatus status;
        if (coverageRatio.compareTo(BigDecimal.valueOf(1.20)) >= 0) {
            status = HealthStatus.HEALTHY;
        } else if (coverageRatio.compareTo(BigDecimal.valueOf(0.80)) >= 0) {
            status = HealthStatus.ATTENTION_NEEDED;
        } else {
            status = HealthStatus.CRITICAL;
        }

        // 2. Liquidity Insight
        if (status == HealthStatus.HEALTHY) {
            insights.add(new AdvisoryInsight(
                    InsightType.LIQUIDITY,
                    Severity.SUCCESS,
                    "Cash Flow & Liquidity Healthy",
                    "பணப்புழக்கம் மற்றும் ரொக்க கையிருப்பு சீராக உள்ளது",
                    "Available funds of ₹" + available + " cover " + coverageRatio + "x of immediate 7-day obligations (₹" + immediateDues + ").",
                    "கையிருப்புத் தொகை ₹" + available + ", அடுத்த 7 நாள் பாக்கிகளுக்கு (₹" + immediateDues + ") " + coverageRatio + " மடங்கு போதுமானது.",
                    "Continue standard payments schedule without borrowing.",
                    "திட்டமிட்டபடி சப்ளையர் பாக்கிகளை செலுத்தலாம்."));
        } else {
            insights.add(new AdvisoryInsight(
                    InsightType.LIQUIDITY,
                    status == HealthStatus.CRITICAL ? Severity.CRITICAL : Severity.WARNING,
                    "Immediate Cash Flow Attention Required",
                    "உடனடி பணப்புழக்க கவனம் தேவை",
                    "Available funds of ₹" + available + " cover only " + coverageRatio + "x of immediate commitments (₹" + immediateDues + ").",
                    "கையிருப்புத் தொகை ₹" + available + ", அடுத்த 7 நாள் பாக்கிகளுக்கு (₹" + immediateDues + ") " + coverageRatio + " மடங்கு மட்டுமே உள்ளது.",
                    "Accelerate customer collections or arrange credit buffer before supplier due dates.",
                    "வாடிக்கையாளர்களிடம் நிலுவைத் தொகையை விரைந்து வசூலிக்கவும்."));
        }

        // 3. Customer Receivables vs Supplier Payables
        BigDecimal receivables = d.customerReceivables() != null ? d.customerReceivables() : BigDecimal.ZERO;
        BigDecimal payables = d.supplierPayables() != null ? d.supplierPayables() : BigDecimal.ZERO;
        if (receivables.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(new AdvisoryInsight(
                    InsightType.RECEIVABLES,
                    Severity.INFO,
                    "Customer Outstanding Inflow Pipeline",
                    "வாடிக்கையாளர் வரவு மற்றும் நிலுவை",
                    "Outstanding receivables of ₹" + receivables + " against supplier payables of ₹" + payables + ".",
                    "மொத்த வாடிக்கையாளர் நிலுவை ₹" + receivables + ", சப்ளையர் பாக்கி ₹" + payables + ".",
                    "Focus collections on overdue accounts to maintain positive operating cash flow.",
                    "பழைய நிலுவைகளை வசூலிப்பதன் மூலம் பண இருப்பை அதிகரிக்கலாம்."));
        }

        // 4. Raw Silk Runway Forecast
        BigDecimal rawSilkKg = d.stock() != null && d.stock().rawSilkKg() != null ? d.stock().rawSilkKg() : BigDecimal.ZERO;
        Integer silkRunwayDays;
        if (rawSilkKg.compareTo(BigDecimal.ZERO) <= 0) {
            silkRunwayDays = 0;
            insights.add(new AdvisoryInsight(
                    InsightType.INVENTORY,
                    Severity.CRITICAL,
                    "Raw Silk Stock Depleted",
                    "கச்சா பட்டு இருப்பு தீர்ந்துவிட்டது",
                    "Raw silk inventory is currently 0 kg.",
                    "தற்போது கச்சா பட்டு இருப்பு இல்லை.",
                    "Place immediate purchase orders to prevent loom and warping idle time.",
                    "உற்பத்தி தடைபடாமல் இருக்க உடனடியாக பட்டு கொள்முதல் செய்யவும்."));
        } else if (rawSilkKg.compareTo(BigDecimal.valueOf(50)) < 0) {
            silkRunwayDays = rawSilkKg.divide(BigDecimal.valueOf(10), 0, RoundingMode.HALF_UP).intValue();
            insights.add(new AdvisoryInsight(
                    InsightType.INVENTORY,
                    Severity.WARNING,
                    "Raw Silk Stock Buffer Low",
                    "கச்சா பட்டு இருப்பு குறைவு",
                    "Current raw silk stock is " + rawSilkKg + " kg (estimated ~" + silkRunwayDays + " days of production).",
                    "தற்போதைய கச்சா பட்டு இருப்பு " + rawSilkKg + " கிலோ (சுமார் " + silkRunwayDays + " நாட்களுக்கு மட்டுமே போதுமானது).",
                    "Plan procurement from silk mandi suppliers this week.",
                    "இந்த வாரத்திற்குள் புதிய பட்டு கொள்முதல் செய்ய திட்டமிடவும்."));
        } else {
            silkRunwayDays = rawSilkKg.divide(BigDecimal.valueOf(10), 0, RoundingMode.HALF_UP).intValue();
            insights.add(new AdvisoryInsight(
                    InsightType.INVENTORY,
                    Severity.SUCCESS,
                    "Raw Silk Inventory Optimal",
                    "கச்சா பட்டு இருப்பு போதுமானது",
                    "Current raw silk stock of " + rawSilkKg + " kg provides ~" + silkRunwayDays + " days of production runway.",
                    "கச்சா பட்டு இருப்பு " + rawSilkKg + " கிலோ உற்பத்திக்கு போதுமானதாக (~" + silkRunwayDays + " நாட்கள்) உள்ளது.",
                    "Maintain current production batch schedules.",
                    "வழக்கம்போல் பேட்ச் தயாரிப்பை தொடரலாம்."));
        }

        Integer cashRunwayDays = available.compareTo(BigDecimal.ZERO) > 0
                ? available.divide(BigDecimal.valueOf(2500), 0, RoundingMode.HALF_UP).intValue()
                : 0;

        return new AiAdvisoryResponse(status, coverageRatio, cashRunwayDays, silkRunwayDays, insights);
    }
}
