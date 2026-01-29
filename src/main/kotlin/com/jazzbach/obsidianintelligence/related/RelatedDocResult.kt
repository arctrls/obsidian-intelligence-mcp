package com.jazzbach.obsidianintelligence.related

data class RelatedDocResult(
    val filePath: String,
    val title: String,
    val score: Double,
    val tags: List<String>
)
