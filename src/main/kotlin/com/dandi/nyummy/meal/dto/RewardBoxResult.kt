package com.dandi.nyummy.meal.dto

import com.dandi.nyummy.mail.enum.RewardBoxCategory

/** 식사 보상으로 뽑은 상자. */
data class RewardBoxResult(val category: RewardBoxCategory, val coinQuantity: Int)
