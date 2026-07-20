package com.dandi.nyummy.meal.service

import com.dandi.nyummy.infra.aws.s3.S3Presigner
import com.dandi.nyummy.meal.dto.CreateMealRequest
import com.dandi.nyummy.meal.dto.CreateMealResponse
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.meal.toEntity
import com.dandi.nyummy.meal.toResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Service
class CreateMealService(
    private val s3Presigner: S3Presigner,
    private val clock: Clock = Clock.System,
    private val mealRepository: MealRepository
) {

    companion object {
        private val PRESIGNED_PUT_URL_EXPIRATION = 10.minutes
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }

    fun getUploadUrl(request: UploadImageRequest): UploadImageResponse {

        val extension = request.fileName.substringAfterLast(".", missingDelimiterValue = "").lowercase()

        require(extension in ALLOWED_EXTENSIONS) {
            "지원하지 않는 파일 형식입니다. (허용: $ALLOWED_EXTENSIONS)"
        }

        require(request.contentType.startsWith("image/")) {
            "이미지 타입만 업로드 가능합니다."
        }

        require(0 < request.fileSizeBytes && request.fileSizeBytes <= 10_485_760) {
            "파일 크기는 최소 0보다 크며, 최대 10MB(10_485_760 Bytes)를 초과할 수 없습니다."
        }

        val s3KeyPath = "meal/${UUID.randomUUID()}.$extension"
        val expirationInstant = clock.now() + PRESIGNED_PUT_URL_EXPIRATION

        val uploadUrl = runCatching {
            s3Presigner.getPutObjectUrl(
                keyName = s3KeyPath,
                type = request.contentType,
                duration = PRESIGNED_PUT_URL_EXPIRATION
            )
        }.getOrElse {
            println("PUT url 반환 실패")
            throw it
        }

        return UploadImageResponse(
            uploadUrl = uploadUrl.toString(),
            imageKey = s3KeyPath,
            uploadMethod = "PUT",
            uploadHeaders = mapOf("Content-Type" to request.contentType),
            expiresAt = expirationInstant,
        )
    }

    fun createMeal(request: CreateMealRequest): CreateMealResponse {

        val meal = request.toEntity()
        val savedMeal = mealRepository.save(meal)

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                createNutrition()
            }.getOrThrow()
        }

        return savedMeal.toResponse()
    }

    suspend fun createNutrition() {
        println("영양소 분석 시작")
        delay(5.seconds)
        println("영양소 분석 끝")
    }

}