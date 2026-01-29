package com.jazzbach.obsidianintelligence.search

data class SearchQuery(
    val text: String,
    val topK: Int = 10,
    val similarityThreshold: Double = 0.3,
    val tags: List<String> = emptyList(),
    val excludePaths: List<String> = emptyList()
)
