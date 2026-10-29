# OpenSpec: Full Localization & Tamil Support Validation (NFR-USE-01, NFR-USE-02, NFR-USE-03, 06 Rule P7)

- **Feature ID:** FEAT-LOCALIZATION-TAMIL
- **Requirements Covered:** NFR-USE-01, NFR-USE-02, NFR-USE-03, NFR-USE-04, 06 Rule P7
- **Milestone:** M20
- **Status:** APPROVED (Spec-Driven Development)
- **Author:** Antigravity Pair Programmer
- **Date:** 2026-10-23

---

## 1. Executive Summary & Business Motivation

SmartSilk (Nexora) is tailored specifically for the silk yarn trading and weaving ecosystem in Tamil Nadu (e.g., Salem, Kanchipuram, Arani, Dharmapuri). While the initial release used English to establish foundational data structures and ledger mechanics (06 rule P7), daily operational efficiency for weavers, warehouse staff, and ledger accountants increases dramatically when operating in Tamil.

This specification implements:
1. **Tamil Resource Bundle (`strings_ta.properties`):** Complete, professional Tamil translations for all operational domain concepts (parties, raw silk, silk warp, yarn counts, purchase receipts, deliveries, payments, ledger errors, reports, and reminders).
2. **Dynamic Locale Engine (`Strings.kt`):** Runtime locale switching (`"en"` <-> `"ta"`) with UTF-8 encoding preservation, argument formatting (`{0}`, `{1}`), and automatic fallback to English when a key is untranslated.
3. **Locale Setting & Persistence:** Support for selecting the user's preferred language (`en` or `ta`).
4. **Validation Test Suite:** Automated verification ensuring UTF-8 Tamil text loading, parameter interpolation, and no UI truncation or missing essential business terms.

---

## 2. Technical Design & Contracts

### 2.1 Dynamic Locale Engine (`Strings.kt`)
- `Strings.setLocale(language: String)`: Loads `strings_<language>.properties` if not English. Falls back to `strings.properties`.
- `Strings.currentLocale(): String`: Returns current active locale code (`"en"` or `"ta"`).
- `Strings.availableLocales(): List<String>`: Returns supported locale codes `["en", "ta"]`.
- `Strings.get(key, *args)` and `s(key, *args)` automatically query the active locale table, falling back to base English if absent.

### 2.2 Resource Bundle (`strings_ta.properties`)
- Stored as UTF-8 in `src/main/resources/strings_ta.properties`.
- Covers core business workflows:
  - Error messages (`error.*`)
  - Common action words (`common.*`)
  - Application setup & authentication (`setup.*`, `login.*`)
  - Domain navigation & tabs (`nav.*`, `parties.*`, `procurement.*`, `orders.*`, `finance.*`, `inventory.*`, `production.*`, `reports.*`)
  - Status enums (`enum.*`)

---

## 3. Quality Gate
- All tests pass in `nexora-desktop` and `Nexora-backend`.
- `StringsTest` and `TamilStringsTest` pass.
- `git diff --check` passes with no whitespace errors.
