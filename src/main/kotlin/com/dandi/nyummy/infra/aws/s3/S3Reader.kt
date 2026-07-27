package com.dandi.nyummy.infra.aws.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.toByteArray
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class S3Reader(
    private val s3Client: S3Client,
    @Value("\${AWS_S3_BUCKET_NAME}") private val s3BucketName: String
) {

    fun getObject(keyName: String): S3ObjectContent {
        val request = GetObjectRequest {
            bucket = s3BucketName
            key = keyName
        }

        return runBlocking {
            s3Client.getObject(request) { response ->
                S3ObjectContent(
                    bytes = response.body?.toByteArray()
                        ?: throw IllegalStateException("S3 객체 바디가 비어 있습니다: $keyName"),
                    contentType = response.contentType
                )
            }
        }
    }
}

data class S3ObjectContent(
    val bytes: ByteArray,
    val contentType: String?
)