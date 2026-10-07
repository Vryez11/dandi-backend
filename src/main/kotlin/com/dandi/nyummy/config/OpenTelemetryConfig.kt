package com.dandi.nyummy.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenTelemetryConfig {
    // Boot는 logback-spring.xml의 OTEL appender에 SDK를 연결해 주지 않는다.
    // install 전에 찍힌 로그(기동 초반)는 OTLP로 나가지 않고 콘솔에만 남는다.
    @Bean
    fun openTelemetryAppenderInitializer(openTelemetry: OpenTelemetry): InitializingBean =
        InitializingBean { OpenTelemetryAppender.install(openTelemetry) }
}
