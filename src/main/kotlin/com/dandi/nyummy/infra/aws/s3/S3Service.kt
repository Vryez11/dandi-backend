package com.dandi.nyummy.infra.aws.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.url.Url
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.S3ErrorCode
import com.dandi.nyummy.infra.aws.s3.dto.S3ObjectContent
import com.dandi.nyummy.infra.aws.s3.dto.S3UploadResult
import kotlinx.coroutines.runBlocking
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.util.*
import kotlin.time.Duration

@Service
class S3Service(
    private val s3Client: S3Client,
    private val clock: Clock,
    @Value("\${AWS_S3_BUCKET_NAME}") private val bucketName: String,
) {
    companion object {
        private const val TEMP_PREFIX = "temp"

        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/heic",
            "image/webp",
        )

        private val MIME_TO_EXTENSION = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/heic" to "heic",
            "image/webp" to "webp",
        )
    }

    private val tika = Tika()

    fun createUploadUrl(
        contentType: String,
        fileSizeBytes: Long,
        maxFileSizeBytes: Long,
        expiration: Duration,
    ): S3UploadResult {
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE)
        }

        if (0 > fileSizeBytes || fileSizeBytes > maxFileSizeBytes) {
            throw BusinessException(S3ErrorCode.FILE_SIZE_EXCEEDED)
        }

        // TODO: 경로에 user_id 추가하기
        val tempExtension = MIME_TO_EXTENSION.getValue(contentType)
        val key =
            "$TEMP_PREFIX/${UUID.randomUUID()}.$tempExtension"
        val url = createPresignedPutUrl(key, contentType, expiration)

        return S3UploadResult(url, key)
    }

    fun confirmUpload(tempKey: String, finalKeyPrefix: String, maxFileSizeBytes: Long): String = runBlocking {
        if (!tempKey.startsWith("$TEMP_PREFIX/")) {
            throw BusinessException(S3ErrorCode.INVALID_KEY)
        }

        val head = try {
            s3Client.headObject {
                bucket = bucketName
                key = tempKey
            }
        } catch (e: NotFound) {
            throw BusinessException(S3ErrorCode.OBJECT_NOT_FOUND)
        }

        val actualSize = head.contentLength ?: 0L

        if (actualSize <= 0 || actualSize > maxFileSizeBytes) {
            deleteObject(tempKey)
            throw BusinessException(S3ErrorCode.FILE_SIZE_EXCEEDED)
        }

        val headerBytes = downloadObjectRange(tempKey, "bytes=0-1023")

        val detectedMimeType = tika.detect(headerBytes)

        val safeExtension = MIME_TO_EXTENSION[detectedMimeType]
            ?: run {
                deleteObject(tempKey)
                throw BusinessException(S3ErrorCode.UNSUPPORTED_CONTENT_TYPE)
            }

        val today = LocalDate.now(clock)

        // TODO: 경로에 user_id 추가
        val finalKey =
            "$finalKeyPrefix/${today.year}/${today.monthValue}/${today.dayOfMonth}/${UUID.randomUUID()}.$safeExtension"

        s3Client.copyObject(
            CopyObjectRequest {
                bucket = bucketName
                copySource = "$bucketName/$tempKey"
                key = finalKey
            },
        )

        deleteObject(tempKey)
        finalKey
    }

    fun createPresignedGetUrl(key: String, duration: Duration): Url {
        val request = GetObjectRequest {
            bucket = bucketName
            this.key = key
        }
        return runBlocking { s3Client.presignGetObject(request, duration) }.url
    }

    private fun createPresignedPutUrl(key: String, contentType: String, duration: Duration): Url {
        val request = PutObjectRequest {
            bucket = bucketName
            this.key = key
            this.contentType = contentType
        }
        return runBlocking { s3Client.presignPutObject(request, duration) }.url
    }

    private suspend fun downloadObjectRange(key: String, byteRange: String): ByteArray {
        val request = GetObjectRequest {
            bucket = bucketName
            this.key = key
            range = byteRange
        }

        return s3Client.getObject(request) { response ->
            response.body?.toByteArray() ?: ByteArray(0)
        }
    }

    private suspend fun deleteObject(key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest {
                bucket = bucketName
                this.key = key
            },
        )
    }

    fun downloadObject(key: String): S3ObjectContent {
        val request = GetObjectRequest {
            bucket = bucketName
            this.key = key
        }

        return runBlocking {
            s3Client.getObject(request) { response ->
                S3ObjectContent(
                    bytes = response.body?.toByteArray()
                        ?: throw IllegalStateException("S3 객체 바디가 비어 있습니다: $key"),
                    contentType = response.contentType,
                )
            }
        }
    }
}
