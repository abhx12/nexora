package com.nexora.documents.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// FR-DOC-06 / Post-MVP Voice & AI: Heuristic parser and extractor for supplier bills, PDFs, and Tamil/English voice transcripts.
public class BillOcrExtractor {

    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(?i)(?:weight|quantity|qty|ordered|எடை|அளவு)?[:\\s]*([0-9]+(?:\\.[0-9]{1,3})?)\\s*(?:kg|kgs|kilo|கிலோகிராம்|கிலோ|கிராம்)|([0-9]+(?:\\.[0-9]{1,3})?)\\s*(?:kg|kgs|கிலோ)\\b");

    private static final Pattern RATE_PATTERN = Pattern.compile(
            "(?i)(?:rate|price|unit\\s*rate|@|விலை|விகிதம்|ரேட்)[:\\s]*(?:rs\\.?|₹|ரூ\\.?|ரூபாய்)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)|(?:rs\\.?|₹|ரூ\\.?|ரூபாய்)\\s*([0-9]+(?:\\.[0-9]{1,2})?)(?:\\s*(?:/|per|கிலோவுக்கு|கிலோ)\\s*(?:kg|கிலோ)?)?|([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:/|per|கிலோவுக்கு)\\s*(?:kg|கிலோ)");

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?i)(?:total|gross|amount|net\\s*amount|bill\\s*amount|மொத்த\\s*தொகை|மொத்தம்|தொகை|பில்\\s*தொகை)[:\\s]*(?:rs\\.?|₹|ரூ\\.?|ரூபாய்)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)");

    private static final Pattern BILL_NUM_PATTERN = Pattern.compile(
            "(?i)(?:bill\\s*(?:no|num|#)?|inv(?:oice)?\\s*(?:no|num|#)?|பில்\\s*(?:எண்|நெம்பர்|நம்பர்|no|num|#)?|ரசீது\\s*(?:எண்|நெம்பர்|நம்பர்|no|num|#)?)[:\\s]*([A-Za-z0-9_-]+)");

    private static final Pattern SUPPLIER_PATTERN = Pattern.compile(
            "(?iu)(?:supplier|from|seller|vendor|அனுப்புநர்|விற்பனையாளர்|வியாபாரி)[:\\s]*([\\p{L}\\p{M}0-9\\s,.-]{2,50}?)(?=[\\r\\n]|\\s*(?:weight|rate|bill|inv|total|gross|date|எடை|விலை|ரேட்|பில்|ரசீது|கடன்|ரொக்கம்|பணம்|ரா\\s*சில்க்|கச்சா\\s*பட்டு|வுடா|vuda|raw\\s*silk)|$)");

    private static final Pattern LEADING_SUPPLIER_PATTERN = Pattern.compile(
            "(?iu)^\\s*([\\p{L}\\p{M}\\s,.-]{2,50}?)(?=\\s*(?:[0-9]|weight|quantity|qty|ordered|rate|price|bill|inv|total|gross|எடை|அளவு|கிலோ|விலை|விகிதம்|ரேட்|பில்|ரசீது|கடன்|ரொக்கம்|ரா\\s*சில்க்|கச்சா\\s*பட்டு|வுடா|vuda|raw\\s*silk))");

    private static final Pattern PRODUCT_RAW_SILK = Pattern.compile("(?iu)(?:ரா\\s*சில்க்|கச்சா\\s*பட்டு|raw\\s*silk)");
    private static final Pattern PRODUCT_VUDA_WARP = Pattern.compile("(?iu)(?:வுடா|வூடா|vuda(?:\\s*warp)?)");

    private static final Pattern PAYMENT_CREDIT = Pattern.compile("(?iu)(?:கடன்|credit)");
    private static final Pattern PAYMENT_CASH = Pattern.compile("(?iu)(?:ரொக்கம்|பணம்|cash)");
    private static final Pattern CREDIT_DAYS = Pattern.compile("(?iu)([0-9]{1,3})\\s*(?:நாட்கள்|தவணை|days(?:\\s*credit)?)");

    public static Map<String, Object> extract(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            return new LinkedHashMap<>();
        }
        String text = extractText(bytes, fileName);
        return extractFromText(text);
    }

    public static Map<String, Object> extractFromText(String rawText) {
        Map<String, Object> suggested = new LinkedHashMap<>();
        if (rawText == null || rawText.isBlank()) {
            return suggested;
        }

        String text = normalizeTamilDigits(rawText).replaceAll("(?<=\\d),(?=\\d)", "");

        // Extract weight
        Matcher weightMatcher = WEIGHT_PATTERN.matcher(text);
        if (weightMatcher.find()) {
            try {
                String match = weightMatcher.group(1);
                if (match == null) {
                    match = weightMatcher.group(2);
                }
                if (match != null) {
                    BigDecimal weight = new BigDecimal(match).setScale(3, RoundingMode.HALF_UP);
                    if (weight.compareTo(BigDecimal.ZERO) > 0) {
                        suggested.put("orderedWeightKg", weight);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Extract rate
        Matcher rateMatcher = RATE_PATTERN.matcher(text);
        while (rateMatcher.find()) {
            try {
                String match = rateMatcher.group(1);
                if (match == null) {
                    match = rateMatcher.group(2);
                }
                if (match == null) {
                    match = rateMatcher.group(3);
                }
                if (match != null) {
                    BigDecimal rate = new BigDecimal(match).setScale(2, RoundingMode.HALF_UP);
                    if (rate.compareTo(BigDecimal.ZERO) > 0) {
                        suggested.put("ratePerKg", rate);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Extract bill number
        Matcher billMatcher = BILL_NUM_PATTERN.matcher(text);
        if (billMatcher.find()) {
            String bNo = billMatcher.group(1).trim();
            if (!bNo.equalsIgnoreCase("no") && !bNo.equalsIgnoreCase("num")) {
                suggested.put("billNumber", bNo);
            }
        }

        // Extract supplier name
        Matcher supplierMatcher = SUPPLIER_PATTERN.matcher(text);
        if (supplierMatcher.find()) {
            String sup = supplierMatcher.group(1).trim();
            if (sup.length() >= 2) {
                suggested.put("supplierName", sup);
            }
        } else {
            Matcher leadingMatcher = LEADING_SUPPLIER_PATTERN.matcher(text);
            if (leadingMatcher.find()) {
                String sup = leadingMatcher.group(1).trim();
                if (sup.length() >= 2) {
                    suggested.put("supplierName", sup);
                }
            }
        }

        // Extract product type
        if (PRODUCT_VUDA_WARP.matcher(text).find()) {
            suggested.put("productType", "VUDA_WARP");
        } else if (PRODUCT_RAW_SILK.matcher(text).find()) {
            suggested.put("productType", "RAW_SILK");
        }

        // Extract payment type & credit days
        if (PAYMENT_CASH.matcher(text).find()) {
            suggested.put("paymentType", "CASH");
            suggested.put("creditDays", 0);
        } else if (PAYMENT_CREDIT.matcher(text).find()) {
            suggested.put("paymentType", "CREDIT");
            Matcher daysMatcher = CREDIT_DAYS.matcher(text);
            if (daysMatcher.find()) {
                try {
                    suggested.put("creditDays", Integer.parseInt(daysMatcher.group(1)));
                } catch (Exception ignored) {
                    suggested.put("creditDays", 15);
                }
            } else {
                suggested.put("creditDays", 15);
            }
        }

        // Extract total or calculate from weight * rate
        Matcher totalMatcher = TOTAL_PATTERN.matcher(text);
        if (totalMatcher.find()) {
            try {
                BigDecimal total = new BigDecimal(totalMatcher.group(1)).setScale(2, RoundingMode.HALF_UP);
                suggested.put("grossAmount", total);
            } catch (Exception ignored) {
            }
        } else if (suggested.containsKey("orderedWeightKg") && suggested.containsKey("ratePerKg")) {
            BigDecimal weight = (BigDecimal) suggested.get("orderedWeightKg");
            BigDecimal rate = (BigDecimal) suggested.get("ratePerKg");
            suggested.put("grossAmount", weight.multiply(rate).setScale(2, RoundingMode.HALF_UP));
        }

        return suggested;
    }

    private static String extractText(byte[] bytes, String fileName) {
        StringBuilder sb = new StringBuilder();
        // If PDF, extract text enclosed in (...) Tj
        if (startsWithPdf(bytes)) {
            Pattern pdfText = Pattern.compile("\\(([^)]+)\\)\\s*Tj");
            String raw = new String(bytes, StandardCharsets.ISO_8859_1);
            Matcher m = pdfText.matcher(raw);
            while (m.find()) {
                sb.append(m.group(1)).append("\n");
            }
        }
        // Also append general UTF-8 text representation
        sb.append("\n").append(new String(bytes, StandardCharsets.UTF_8));
        if (fileName != null) {
            sb.append(" ").append(fileName);
        }
        return sb.toString();
    }

    private static boolean startsWithPdf(byte[] b) {
        return b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
    }

    private static String normalizeTamilDigits(String text) {
        if (text == null) return "";
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= '\u0BE6' && chars[i] <= '\u0BEF') {
                chars[i] = (char) ('0' + (chars[i] - '\u0BE6'));
            }
        }
        return new String(chars);
    }
}
