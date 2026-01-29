package com.jazzbach.obsidianintelligence.duplicate

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.duplicate")
data class DuplicateProperties(
    val similarityThreshold: Double = 0.85,
    val minWordCount: Int = 50
)
