package com.dandi.nyummy.infra.aws.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class S3Config(
    @Value("\${AWS_ACCESS_KEY}") private val accessKey: String,
    @Value("\${AWS_SECRET_ACCESS_KEY}") private val secretKey: String,
    @Value("\${AWS_REGION}") private val region: String
) {
    @Bean
    fun s3Client(): S3Client {
        return S3Client {
            this.region = this@S3Config.region
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = accessKey
                secretAccessKey = secretKey
            }
        }
    }
}