package com.jazzbach.obsidianintelligence.search

data class SearchResult(
    val filePath: String,
    val title: String,
    val score: Double,
    val snippet: String,
    val tags: List<String>,
    val wordCount: Int
)
