package com.dandi.nyummy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@ConfigurationPropertiesScan
@EnableJpaAuditing
@SpringBootApplication(
    exclude = [UserDetailsServiceAutoConfiguration::class],
)
class NyummyApplication

fun main(args: Array<String>) {
    runApplication<NyummyApplication>(*args)
}
