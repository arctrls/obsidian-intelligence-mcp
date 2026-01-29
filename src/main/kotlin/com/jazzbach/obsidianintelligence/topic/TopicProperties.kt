package com.jazzbach.obsidianintelligence.topic

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.topic")
data class TopicProperties(
    val defaultTopK: Int = 50,
    val similarityThreshold: Double = 0.3,
    val minGroupSize: Int = 2
)
