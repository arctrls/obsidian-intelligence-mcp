package com.jazzbach.obsidianintelligence.related

interface UpdateRelatedSection {
    fun update(filePath: String, topK: Int = 5): RelatedSectionResult
}

data class RelatedSectionResult(
    val filePath: String,
    val relatedDocs: List<RelatedDocResult>,
    val sectionAdded: Boolean,
    val success: Boolean,
    val errorMessage: String? = null
)
