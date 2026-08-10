package com.dandi.nyummy.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

class ArgonProvider(private val argonProperties: ArgonProperties) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = Argon2PasswordEncoder(
        argonProperties.saltLength,
        argonProperties.hashLength,
        argonProperties.parallelism,
        argonProperties.memory,
        argonProperties.iterations,
    )
}

@ConfigurationProperties("app.argon")
class ArgonProperties(

    val saltLength: Int,
    val hashLength: Int,
    val parallelism: Int,
    val memory: Int,
    val iterations: Int,
)
