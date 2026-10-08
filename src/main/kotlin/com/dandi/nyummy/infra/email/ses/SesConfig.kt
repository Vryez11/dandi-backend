package com.dandi.nyummy.infra.email.ses

import aws.sdk.kotlin.services.sesv2.SesV2Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SesConfig(@Value("\${AWS_REGION}") private val region: String) {
    @Bean
    fun sesV2Client(): SesV2Client = SesV2Client {
        this.region = this@SesConfig.region
    }
}
