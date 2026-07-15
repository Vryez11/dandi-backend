package com.dandi.nyummy.meal.service

import com.dandi.nyummy.infra.aws.s3.S3Presigner
import com.dandi.nyummy.meal.dto.UploadImageRequest
import com.dandi.nyummy.meal.dto.UploadImageResponse
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Service
class CreateMealService(
    private val s3Presigner: S3Presigner,
    private val clock: Clock = Clock.System
) {

    companion object {
        private val PRESIGNED_PUT_URL_EXPIRATION = 10.minutes
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }

    suspend fun getUploadUrl(request: UploadImageRequest): UploadImageResponse {

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

        val uploadUrl = s3Presigner.getPutObjectUrl(
            keyName = s3KeyPath,
            type = request.contentType,
            duration = PRESIGNED_PUT_URL_EXPIRATION
        )

        return UploadImageResponse(
            uploadUrl = uploadUrl.toString(),
            imageKey = s3KeyPath,
            uploadMethod = "PUT",
            uploadHeaders = mapOf("Content-Type" to request.contentType),
            expiresAt = expirationInstant
        )
    }

}