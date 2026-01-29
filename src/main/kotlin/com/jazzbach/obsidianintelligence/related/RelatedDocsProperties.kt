package com.jazzbach.obsidianintelligence.related

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.related-docs")
data class RelatedDocsProperties(
    val defaultTopK: Int = 5,
    val sectionTitle: String = "## 관련 문서",
    val similarityThreshold: Double = 0.3
)
