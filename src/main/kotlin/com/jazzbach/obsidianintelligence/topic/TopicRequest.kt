package com.jazzbach.obsidianintelligence.topic

data class TopicRequest(
    val topic: String,
    val topK: Int = 50,
    val similarityThreshold: Double = 0.3,
    val minWordCount: Int = 0,
    val filterTags: List<String> = emptyList()
)
