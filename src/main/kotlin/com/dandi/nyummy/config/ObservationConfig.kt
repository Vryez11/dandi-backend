package com.dandi.nyummy.config

import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Configuration
class ObservationConfig {
    // actuator 요청(ALB·compose health check)은 메트릭·트레이스를 만들지 않는다.
    // HTTP 요청 관측은 context에서 경로를 꺼낸다. 그 안쪽 관측(Spring Security 필터 체인 등)은
    // 부모가 빠지면 별도 trace로 떨어져 나오므로, 현재 요청 경로로 같이 거른다.
    @Bean
    fun actuatorObservationPredicate(): ObservationPredicate = ObservationPredicate { _, context ->
        val request = (context as? ServerRequestObservationContext)?.carrier
            ?: (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        request?.requestURI?.startsWith("/actuator") != true
    }
}
