package com.dandi.nyummy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class NyummyApplication

fun main(args: Array<String>) {
    runApplication<NyummyApplication>(*args)
}
