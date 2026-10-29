package com.nexora.documents.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillOcrExtractorTest {

    @Test
    @DisplayName("Tamil voice transcript: extracts supplier, weight, rate, bill number, payment type, credit days, and gross amount")
    void tamilTranscript_extractsAllFields() {
        String tamilText = "ஸ்ரீ பாலாஜி சில்க்ஸ் 25.500 கிலோ ரேட் 4500 பில் எண் 88 கடன் 15 நாட்கள்";
        Map<String, Object> result = BillOcrExtractor.extractFromText(tamilText);

        assertThat(result.get("supplierName")).isEqualTo("ஸ்ரீ பாலாஜி சில்க்ஸ்");
        assertThat(result.get("orderedWeightKg")).isEqualTo(new BigDecimal("25.500"));
        assertThat(result.get("ratePerKg")).isEqualTo(new BigDecimal("4500.00"));
        assertThat(result.get("billNumber")).isEqualTo("88");
        assertThat(result.get("paymentType")).isEqualTo("CREDIT");
        assertThat(result.get("creditDays")).isEqualTo(15);
        assertThat(result.get("grossAmount")).isEqualTo(new BigDecimal("114750.00"));
    }

    @Test
    @DisplayName("Tamil bill text with cash payment and product type: extracts cash and raw silk")
    void tamilBillText_withCashAndProduct() {
        String text = "விற்பனையாளர்: முருகன் மண்டி ரா சில்க் எடை: 30 கிலோ விலை: ₹4,200 ரசீது எண்: 104 ரொக்கம்";
        Map<String, Object> result = BillOcrExtractor.extractFromText(text);

        assertThat(result.get("supplierName")).isEqualTo("முருகன் மண்டி");
        assertThat(result.get("productType")).isEqualTo("RAW_SILK");
        assertThat(result.get("orderedWeightKg")).isEqualTo(new BigDecimal("30.000"));
        assertThat(result.get("ratePerKg")).isEqualTo(new BigDecimal("4200.00"));
        assertThat(result.get("billNumber")).isEqualTo("104");
        assertThat(result.get("paymentType")).isEqualTo("CASH");
        assertThat(result.get("creditDays")).isEqualTo(0);
        assertThat(result.get("grossAmount")).isEqualTo(new BigDecimal("126000.00"));
    }

    @Test
    @DisplayName("Tamil numerals (௨௫.௫ கிலோ) are normalized and extracted correctly")
    void tamilNumerals_areNormalized() {
        String text = "எடை: ௨௫.௫ கிலோ ரேட்: ௪௨௦௦";
        Map<String, Object> result = BillOcrExtractor.extractFromText(text);

        assertThat(result.get("orderedWeightKg")).isEqualTo(new BigDecimal("25.500"));
        assertThat(result.get("ratePerKg")).isEqualTo(new BigDecimal("4200.00"));
        assertThat(result.get("grossAmount")).isEqualTo(new BigDecimal("107100.00"));
    }

    @Test
    @DisplayName("English bill text: preserves legacy extraction behavior")
    void englishBill_extractsCorrectly() {
        String english = "Supplier: Silk Exporters Weight: 250.000 kg Rate: 800.00 / kg Bill No: INV-2026-09";
        Map<String, Object> result = BillOcrExtractor.extract(english.getBytes(StandardCharsets.UTF_8), "bill.txt");

        assertThat(result.get("supplierName")).isEqualTo("Silk Exporters");
        assertThat(result.get("orderedWeightKg")).isEqualTo(new BigDecimal("250.000"));
        assertThat(result.get("ratePerKg")).isEqualTo(new BigDecimal("800.00"));
        assertThat(result.get("billNumber")).isEqualTo("INV-2026-09");
        assertThat(result.get("grossAmount")).isEqualTo(new BigDecimal("200000.00"));
    }
}
