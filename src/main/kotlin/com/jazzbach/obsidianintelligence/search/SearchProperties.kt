package com.jazzbach.obsidianintelligence.search

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.search")
data class SearchProperties(
    val defaultTopK: Int = 10,
    val similarityThreshold: Double = 0.3,
    val hybridDenseWeight: Double = 0.7,
    val hybridKeywordWeight: Double = 0.3
)
