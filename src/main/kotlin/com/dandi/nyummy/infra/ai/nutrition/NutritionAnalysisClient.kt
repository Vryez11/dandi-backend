package com.dandi.nyummy.infra.ai.nutrition

import com.dandi.nyummy.meal.dto.Nutrition

interface NutritionAnalysisClient {

    fun analyzeNutrition(imageKey: String): Nutrition
}