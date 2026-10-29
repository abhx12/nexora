package com.nexora.reporting.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import tools.jackson.databind.JsonNode;

import com.nexora.support.AbstractApiTest;
import com.nexora.support.AuthTokens;

class DashboardApiTest extends AbstractApiTest {

    @Test
    @DisplayName("FR-RPT-01 FR-NTF-06 summary returns cash receivables payables stock and dues")
    void summary_returnsCashReceivablesPayablesStockAndDues() throws Exception {
        // AC3: an empty database gives zeros and empty lists
        asOwner(get("/api/v1/dashboard/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(0.0)).andExpect(jsonPath("$.totalAvailable").value(0.0))
                .andExpect(jsonPath("$.customerReceivables").value(0.0))
                .andExpect(jsonPath("$.supplierPayables").value(0.0))
                .andExpect(jsonPath("$.loanOutstandingPrincipal").value(0.0))
                .andExpect(jsonPath("$.stock.rawSilkKg").value(0.0))
                .andExpect(jsonPath("$.ordersByStatus.PLACED").value(0))
                .andExpect(jsonPath("$.banks.length()").value(0))
                .andExpect(jsonPath("$.upcomingSupplierDues.length()").value(0))
                .andExpect(jsonPath("$.recentTransactions.length()").value(0));

        String cash = createAccount("Cash", "CASH", "80000.00");
        String bank = createAccount("SBI Current", "BANK", "150000.00");

        // supplier dues: overdue by 3 days, due in 3 days (listed, FR-NTF-06 AC1), due in 20 days (not listed, AC2)
        String supplier = data.supplier().id().toString();
        String overdue = receivedPurchase(supplier, today().minusDays(10), 7, "10.000", "1000.00");
        String soon = receivedPurchase(supplier, today(), 3, "20.000", "500.00");
        receivedPurchase(supplier, today(), 20, "10.000", "500.00");

        // a customer owes 20000 - 5000; another has paid 3000 ahead (an advance)
        UUID customerId = data.customer().id();
        String customer = customerId.toString();
        confirmedOrder(data.customer().id(), "RAW_SILK", "10.000", "2000.00");
        confirmedOrder(customerId, "RAW_SILK", "10.000", "2000.00");
        pay("CUSTOMER", customer, "5000.00", "UPI", bank);
        pay("CUSTOMER", data.customer().id().toString(), "3000.00", "CASH", cash);

        // a late PLACED order, a bank loan of 100000, an active chit of 25000 a month, a planned batch, a created job
        asOwner(postJson("/api/v1/orders", """
                {"customerId":"%s","orderDate":"%s","requiredDate":"%s",
                 "items":[{"productType":"KORA_WARP","requiredWeightKg":5.000,"ratePerKg":900.00}]}"""
                .formatted(customer, today().minusDays(5), today().minusDays(1)))).andExpect(status().isCreated());
        asOwner(postWithKey("/api/v1/loans", """
                {"lenderId":"%s","loanType":"BANK","principalAmount":100000.00,"startDate":"%s",
                 "financialAccountId":"%s","paymentMethod":"BANK_TRANSFER"}"""
                .formatted(data.lender().id(), today(), bank))).andExpect(status().isCreated());
        asOwner(postJson("/api/v1/chits", """
                {"chitName":"Chit A","totalAmount":500000.00,"monthlyAmount":25000.00,"durationMonths":20,
                 "startDate":"%s","priorContributionsCount":0,"priorContributionsAmount":0.00}""".formatted(today())))
                .andExpect(status().isCreated());
        asOwner(postJson("/api/v1/production-batches", """
                {"productType":"KORA_WARP","productionDate":"%s","inputWeightKg":10.000}""".formatted(today())))
                .andExpect(status().isCreated());
        asOwner(postJson("/api/v1/outsourcing-jobs", """
                {"manufacturerId":"%s","productType":"SILK_WARP","issuedWeightKg":5.000,"ratePerKg":100.00}"""
                .formatted(data.manufacturer().id()))).andExpect(status().isCreated());

        JsonNode d = body(asOwner(get("/api/v1/dashboard/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(83000.00))                         // 80000 + 3000 advance
                .andExpect(jsonPath("$.banks[0].balance").value(255000.00))             // 150000 + 5000 + 100000 loan
                .andExpect(jsonPath("$.totalAvailable").value(338000.00))
                .andExpect(jsonPath("$.customerReceivables").value(35000.00))           // 20000 + 15000
                .andExpect(jsonPath("$.customerAdvances").value(3000.00))
                .andExpect(jsonPath("$.supplierPayables").value(25000.00))              // 10000 + 10000 + 5000
                .andExpect(jsonPath("$.supplierOverdue").value(10000.00))
                .andExpect(jsonPath("$.loanOutstandingPrincipal").value(100000.00))
                .andExpect(jsonPath("$.chitMonthlyCommitments").value(25000.00))
                .andExpect(jsonPath("$.stock.rawSilkKg").value(40.000))
                .andExpect(jsonPath("$.ordersByStatus.PLACED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.CONFIRMED").value(2))
                .andExpect(jsonPath("$.lateOrders").value(1))
                .andExpect(jsonPath("$.productionBatches.PLANNED").value(1))
                .andExpect(jsonPath("$.openOutsourcingJobs").value(1))
                .andExpect(jsonPath("$.upcomingSupplierDues.length()").value(2))
                .andExpect(jsonPath("$.upcomingSupplierDues[0].purchaseId").value(overdue))
                .andExpect(jsonPath("$.upcomingSupplierDues[0].overdue").value(true))
                .andExpect(jsonPath("$.upcomingSupplierDues[0].overdueDays").value(3))
                .andExpect(jsonPath("$.upcomingSupplierDues[1].purchaseId").value(soon))
                .andExpect(jsonPath("$.upcomingSupplierDues[1].overdue").value(false))
                .andExpect(jsonPath("$.upcomingSupplierDues[1].supplier.id").value(supplier))
                .andExpect(jsonPath("$.recentTransactions.length()").value(5))
                .andExpect(jsonPath("$.asOf").isNotEmpty()));

        // AC1: each figure equals the matching dedicated endpoint
        JsonNode position = body(asOwner(get("/api/v1/finance/cash-position")));
        assertThat(d.get("totalAvailable").decimalValue()).isEqualByComparingTo(position.get("totalAvailable").decimalValue());
        JsonNode stock = body(asOwner(get("/api/v1/inventory/summary")));
        assertThat(d.get("stock").get("rawSilkKg").decimalValue())
                .isEqualByComparingTo(stock.get("rawSilkAvailableKg").decimalValue());
        JsonNode payables = body(asOwner(get("/api/v1/payables/suppliers")));
        assertThat(d.get("supplierPayables").decimalValue()).isEqualByComparingTo(payables.get("totalPayable").decimalValue());
        assertThat(d.get("supplierOverdue").decimalValue()).isEqualByComparingTo(payables.get("totalOverdue").decimalValue());
        assertThat(d.get("customerReceivables").decimalValue()).isEqualByComparingTo(
                body(asOwner(get("/api/v1/customers/" + customer + "/outstanding"))).get("totalOutstanding")
                        .decimalValue().add(new BigDecimal("20000.00")));
        assertThat(d.get("loanOutstandingPrincipal").decimalValue()).isEqualByComparingTo(
                body(asOwner(get("/api/v1/loans"))).get("content").get(0).get("outstandingPrincipal").decimalValue());

        // AC2: STAFF has no REPORT_VIEW
        mockMvc.perform(get("/api/v1/dashboard/summary").header(HttpHeaders.AUTHORIZATION,
                AuthTokens.bearer(tokens.staff()))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("FR-RPT-02 obligations never added to available money")
    void obligations_neverAddedToAvailableMoney() throws Exception {
        // cash 80000; a customer owes 500000; the business owes a supplier 300000
        createAccount("Cash", "CASH", "80000.00");
        confirmedOrder(data.customer().id(), "RAW_SILK", "500.000", "1000.00");
        receivedPurchase(data.supplier().id().toString(), today(), 30, "300.000", "1000.00");

        // AC1 + AC2: available money stays 80000; receivables and payables are separate figures
        asOwner(get("/api/v1/dashboard/summary")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(80000.00))
                .andExpect(jsonPath("$.totalAvailable").value(80000.00))
                .andExpect(jsonPath("$.customerReceivables").value(500000.00))
                .andExpect(jsonPath("$.supplierPayables").value(300000.00));
    }

    @Test
    @DisplayName("AI Advisory returns health status, liquidity coverage, runway, and bilingual insights")
    void aiAdvisory_returnsHealthStatusAndInsights() throws Exception {
        asOwner(get("/api/v1/dashboard/ai-advisory")).andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").isNotEmpty())
                .andExpect(jsonPath("$.liquidityCoverageRatio").isNumber())
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.insights[0].titleEn").isNotEmpty())
                .andExpect(jsonPath("$.insights[0].titleTa").isNotEmpty());

        mockMvc.perform(get("/api/v1/dashboard/ai-advisory")
                .header(HttpHeaders.AUTHORIZATION, AuthTokens.bearer(tokens.staff())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // A credit purchase with its own credit days, fully received and accepted; returns its id.
    private String receivedPurchase(String supplier, LocalDate date, int creditDays, String kg, String rate)
            throws Exception {
        String id = body(asOwner(postJson("/api/v1/purchases", """
                {"supplierId":"%s","purchaseDate":"%s","orderedWeightKg":%s,"ratePerKg":%s,"paymentType":"CREDIT",
                 "creditDays":%d}""".formatted(supplier, date, kg, rate, creditDays))).andExpect(status().isCreated()))
                .get("id").asString();
        asOwner(postWithKey("/api/v1/purchases/" + id + "/receipts", """
                {"receiptDate":"%s","receivedWeightKg":%s,"acceptedWeightKg":%s,"rejectedWeightKg":0.000}"""
                .formatted(today(), kg, kg))).andExpect(status().isCreated());
        return id;
    }

    private void pay(String purpose, String party, String amount, String method, String account) throws Exception {
        asOwner(postWithKey("/api/v1/payments", """
                {"purpose":"%s","partyId":"%s","amount":%s,"paymentMethod":"%s","financialAccountId":"%s"}"""
                .formatted(purpose, party, amount, method, account))).andExpect(status().isCreated());
    }
}
