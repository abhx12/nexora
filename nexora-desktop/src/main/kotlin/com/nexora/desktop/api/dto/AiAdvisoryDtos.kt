package com.nexora.desktop.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class AiAdvisoryResponse(
    val healthStatus: String,
    val liquidityCoverageRatio: Double? = null,
    val cashRunwayDays: Int? = null,
    val silkRunwayDays: Int? = null,
    val insights: List<AdvisoryInsight> = emptyList(),
)

@Serializable
data class AdvisoryInsight(
    val type: String,
    val severity: String,
    val titleEn: String,
    val titleTa: String,
    val detailEn: String,
    val detailTa: String,
    val actionableStepEn: String,
    val actionableStepTa: String,
)
