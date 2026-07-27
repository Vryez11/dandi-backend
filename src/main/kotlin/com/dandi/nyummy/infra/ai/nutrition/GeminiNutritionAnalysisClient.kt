package com.dandi.nyummy.infra.ai.nutrition

import com.dandi.nyummy.infra.ai.AiProperties
import com.dandi.nyummy.meal.dto.Nutrition
import org.springframework.web.client.RestClient

class GeminiNutritionAnalyzeNutrition(
    private val restClient: RestClient,
    private val aiProperties: AiProperties
) : NutritionAnalysisClient {

    override fun analyzeNutrition(imageUrl: String): Nutrition {


    }
}