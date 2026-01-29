plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jazzbach"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.spring.ai.get()}")
    }
}

dependencies {
    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)

    // Spring AI MCP Server (Streamable HTTP)
    implementation(libs.spring.ai.mcp.server.webmvc)

    // Spring AI Embedding (ONNX)
    implementation(libs.spring.ai.transformers)

    // Spring AI PgVector Store
    implementation(libs.spring.ai.pgvector)

    // Kotlin
    implementation(libs.kotlin.reflect)

    // PostgreSQL
    runtimeOnly(libs.postgresql)

    // Markdown parsing
    implementation(libs.flexmark.all)

    // YAML frontmatter
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)

    // Test
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.h2)
    testImplementation(libs.testcontainers.postgresql)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
