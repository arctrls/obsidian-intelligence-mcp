package com.jazzbach.obsidianintelligence.topic

data class TopicCollection(
    val topic: String,
    val totalDocuments: Int,
    val groups: List<TopicGroup>,
    val statistics: TopicStatistics,
    val relatedTopics: List<String>
)

data class TopicGroup(
    val tagName: String,
    val documents: List<TopicDocument>
)

data class TopicDocument(
    val filePath: String,
    val title: String,
    val score: Double,
    val tags: List<String>,
    val wordCount: Int,
    val snippet: String
)

data class TopicStatistics(
    val minWordCount: Int,
    val maxWordCount: Int,
    val avgWordCount: Double,
    val tagFrequency: List<Pair<String, Int>>
)
