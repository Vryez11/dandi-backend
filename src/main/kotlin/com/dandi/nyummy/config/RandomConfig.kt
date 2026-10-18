package com.dandi.nyummy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.random.Random

// Clock처럼 빈으로 주입해 테스트에서 뽑기 결과를 고정할 수 있게 한다.
@Configuration
class RandomConfig {
    @Bean
    fun random(): Random = Random.Default
}
