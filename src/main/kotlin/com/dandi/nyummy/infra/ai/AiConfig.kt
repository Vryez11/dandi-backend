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

    // Boot가 주입하는 Builder를 써야 관측(http.client.requests 메트릭·span)이 붙는다.
    // RestClient.builder()로 직접 만들면 trace에서 Gemini 호출 구간이 빈칸으로 남는다.
    @Bean
    fun aiRestClient(aiProperties: AiProperties, restClientBuilder: RestClient.Builder): RestClient {
        val settings = HttpClientSettings.defaults()
            .withConnectTimeout(aiProperties.connectTimeout)
            .withReadTimeout(aiProperties.readTimeout)

        return restClientBuilder
            .baseUrl(aiProperties.baseUrl)
            .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
            .build()
    }
}
