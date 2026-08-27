package com.dandi.nyummy.infra.aws.ses

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.sesv2.SesV2Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SesConfig(
    @Value("\${AWS_ACCESS_KEY}") private val accessKey: String,
    @Value("\${AWS_SECRET_ACCESS_KEY}") private val secretKey: String,
    @Value("\${AWS_REGION}") private val region: String,
) {
    @Bean
    fun sesV2Client(): SesV2Client = SesV2Client {
        this.region = this@SesConfig.region
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }
    }
}
