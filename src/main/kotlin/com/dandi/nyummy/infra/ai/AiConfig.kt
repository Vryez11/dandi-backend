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

    // Boot가 등록한 RestClient.Builder를 받아 쓴다. 관측 customizer가 적용돼 있어
    // Gemini 호출마다 http.client.requests 메트릭과 span이 남는다. RestClient.builder()로 직접 만들면 둘 다 빠진다.
    @Bean
    fun aiRestClient(restClientBuilder: RestClient.Builder, aiProperties: AiProperties): RestClient {
        val settings = HttpClientSettings.defaults()
            .withConnectTimeout(aiProperties.connectTimeout)
            .withReadTimeout(aiProperties.readTimeout)

        return restClientBuilder
            .baseUrl(aiProperties.baseUrl)
            .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
            .build()
    }
}
