package com.jazzbach.obsidianintelligence

import org.springframework.boot.fromApplication
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.PostgreSQLContainer

@Configuration(proxyBeanMethods = false)
class TestContainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("obsidian_intelligence")
            .withUsername("obsidian")
            .withPassword("obsidian")
            .withInitScript("init-pgvector.sql")
}

fun main(args: Array<String>) {
    fromApplication<ObsidianIntelligenceApplication>()
        .with(TestContainersConfiguration::class)
        .run(*args, "--spring.profiles.active=dev")
}
