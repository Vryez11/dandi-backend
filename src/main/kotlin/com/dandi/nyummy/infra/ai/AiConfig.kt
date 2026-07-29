package com.dandi.nyummy.infra.ai

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(AiProperties::class)
class AiConfig {

    @Bean
    fun aiRestClient(aiProperties: AiProperties): RestClient {
        val settings = HttpClientSettings.defaults()
            .withConnectTimeout(aiProperties.connectTimeout)
            .withReadTimeout(aiProperties.readTimeout)

        return RestClient.builder()
            .baseUrl(aiProperties.baseUrl)
            .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
            .build()
    }
}
