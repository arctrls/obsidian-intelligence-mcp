package com.jazzbach.obsidianintelligence.tagging

data class TaggingResult(
    val filePath: String,
    val originalTags: List<String>,
    val generatedTags: List<String>,
    val categorizedTags: Map<TagCategory, List<String>>,
    val success: Boolean,
    val errorMessage: String? = null
)
