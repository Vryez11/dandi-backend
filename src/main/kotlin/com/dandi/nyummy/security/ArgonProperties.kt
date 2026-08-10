package com.dandi.nyummy.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.argon")
class ArgonProperties(

    val saltLength: Int,
    val hashLength: Int,
    val parallelism: Int,
    val memory: Int,
    val iterations: Int,
)
