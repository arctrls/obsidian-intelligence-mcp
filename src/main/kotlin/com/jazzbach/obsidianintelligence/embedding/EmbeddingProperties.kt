package com.jazzbach.obsidianintelligence.embedding

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.embedding")
data class EmbeddingProperties(
    val batchSize: Int = 10
)
