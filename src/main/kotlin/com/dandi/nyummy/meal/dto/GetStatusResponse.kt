package com.dandi.nyummy.meal.dto

import com.dandi.nyummy.meal.domain.Nutrition

data class GetStatusResponse(
    val id: Long,
    val status: String,
    val nutrition: Nutrition?,
)
