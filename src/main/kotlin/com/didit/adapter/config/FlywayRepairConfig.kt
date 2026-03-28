package com.didit.adapter.config

import org.flywaydb.core.Flyway
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayRepairConfig {
    @Bean
    fun flywayRepair(flyway: Flyway): CommandLineRunner =
        CommandLineRunner {
            if (System.getenv("FLYWAY_REPAIR") == "true") {
                flyway.repair()
            }
        }
}
