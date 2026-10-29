package com.nexora.desktop.api.dto

import kotlinx.serialization.Serializable

// Shapes of 05-API-SPEC sections 17 (notifications) and 18 (documents). Reports are read as raw JSON (see ReportRepository).

@Serializable
data class NotificationResponse(
    val id: String,
    val notificationType: String,
    val kind: String? = null,
    val priority: String? = null,
    val party: PartyRef? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val referenceNumber: String? = null,
    val message: String? = null,
    val scheduledAt: String? = null,
    val sentAt: String? = null,
    val status: String,
)

@Serializable
data class NotificationConfig(
    val notificationType: String,
    val enabled: Boolean,
    val daysBeforeDue: Int,
    val overdueRepeatDays: Int,
    val priority: String,
)

@Serializable
data class NotificationConfigRequest(val enabled: Boolean, val daysBeforeDue: Int, val overdueRepeatDays: Int, val priority: String)

@Serializable
data class DocumentResponse(
    val id: String,
    val documentType: String,
    val fileName: String,
    val contentType: String? = null,
    val sizeBytes: Long = 0,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdAt: String? = null,
    val createdBy: String? = null,
)

@Serializable
data class WorkerEfficiencyRow(
    val worker: PartyRef,
    val kgPerHour: Double,
    val hours: Double,
    val kg: Double,
)

@Serializable
data class ProfitAndLossReportLine(
    val period: String,
    val revenue: Double,
    val businessExpenses: Double,
    val interestCost: Double,
    val productionCost: Double,
    val profit: Double,
)

@Serializable
data class ProfitabilityReportLine(
    val key: String,
    val id: String? = null,
    val revenue: Double,
    val cost: Double,
    val margin: Double,
    val marginPercent: Double,
)

@Serializable
data class DocumentShareRequest(
    val partyId: String,
    val message: String? = null,
)

@Serializable
data class TextParseRequest(
    val text: String,
)

@Serializable
data class DocumentOcrRequest(
    val documentId: String,
)

@Serializable
data class OcrExtractionResponse(
    val documentId: String? = null,
    val suggested: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap(),
)
