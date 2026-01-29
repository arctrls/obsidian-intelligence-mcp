package com.jazzbach.obsidianintelligence.tagging

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.tagging")
data class TaggingProperties(
    val maxTagsPerDocument: Int = 10,
    val minSemanticSimilarity: Double = 0.3,
    val maxTopicTags: Int = 4,
    val maxDoctypeTags: Int = 1,
    val maxSourceTags: Int = 1,
    val maxPatternTags: Int = 3,
    val maxFrameworkTags: Int = 2
)
