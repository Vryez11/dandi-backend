package com.dandi.nyummy.config

import io.micrometer.observation.ObservationPredicate
import net.ttddyy.observation.tracing.DataSourceBaseContext
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.ReadinessState
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

    // 앱 기동 중(Flyway 마이그레이션 확인, JPA 초기화) JDBC 관측은 요청과 무관한 trace로 떨어져 나온다.
    // Flyway의 performance_schema 탐색 쿼리는 실패로 남아 배포마다 실패 trace에 섞이므로 기동이 끝나기 전에는 만들지 않는다.
    // "부모가 없으면 거른다"가 아니라 기동 시점으로만 한정해, 요청 밖에서 도는 작업(배치·Worker)의 DB 관측은 그대로 남긴다.
    @Bean
    fun startupJdbcObservationPredicate(applicationAvailability: ApplicationAvailability): ObservationPredicate =
        ObservationPredicate { _, context ->
            context !is DataSourceBaseContext ||
                applicationAvailability.readinessState == ReadinessState.ACCEPTING_TRAFFIC
        }
}
