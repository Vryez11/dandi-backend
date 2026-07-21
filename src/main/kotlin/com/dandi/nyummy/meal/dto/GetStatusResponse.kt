package com.dandi.nyummy.meal.dto

data class GetStatusResponse(
    val id: Long,
    val status: String,
    val nutrition: Nutrition?,
)
