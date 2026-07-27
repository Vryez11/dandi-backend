package com.dandi.nyummy.infra.ai.nutrition

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * 실제 S3 객체 + 실제 Gemini API를 관통하는 수동 스모크 테스트.
 * 실행 조건: .env에 GEMINI_API_KEY·AWS 자격증명, MySQL 컨테이너 기동,
 * 버킷에 IMAGE_KEY 객체 존재.
 * 통과 확인 후 @Disabled 처리할 것 (실행마다 실제 API 호출 발생).
 */
@SpringBootTest
class GeminiNutritionAnalysisClientSmokeTest {

    companion object {
        private const val IMAGE_KEY = "sandwich.jpg"
    }

    @Autowired
    lateinit var geminiNutritionAnalysisClient: GeminiNutritionAnalysisClient

    @Test
    fun `실제 S3 이미지로 Gemini 영양 분석을 수행한다`() {
        val nutrition = geminiNutritionAnalysisClient.analyzeNutrition(IMAGE_KEY)

        println("분석 결과: $nutrition")
        assertTrue(nutrition.calory > 0) { "칼로리가 0 이하입니다: $nutrition" }
        assertTrue(nutrition.carbs >= 0)
        assertTrue(nutrition.protein >= 0)
        assertTrue(nutrition.fat >= 0)
    }
}
