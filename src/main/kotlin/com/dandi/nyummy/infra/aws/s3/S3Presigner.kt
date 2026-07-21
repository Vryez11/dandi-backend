package com.dandi.nyummy.infra.aws.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.sdk.kotlin.services.s3.presigners.presignPutObject
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.time.Duration

@Component
class S3Presigner(
    private val s3Client: S3Client,
    @Value("\${AWS_S3_BUCKET_NAME}") private val bucketName: String
) {

    fun getPutObjectUrl(keyName: String, type: String, duration: Duration): Url {
        val unsignedRequest = PutObjectRequest {
            bucket = bucketName
            key = keyName
            contentType = type
        }

        val presignedRequest = runBlocking {
            runCatching {
                s3Client.presignPutObject(unsignedRequest, duration)
            }.getOrThrow()
        }

        return presignedRequest.url
    }

    fun getGetObjectUrl(keyName: String, duration: Duration): Url {
        val unsignedRequest =
            GetObjectRequest {
                bucket = bucketName
                key = keyName
            }

        val presignedRequest = runBlocking {
            runCatching {
                s3Client.presignGetObject(unsignedRequest, duration)
            }.getOrThrow()
        }

        return presignedRequest.url
    }
}