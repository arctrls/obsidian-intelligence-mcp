package com.jazzbach.obsidianintelligence.related

interface FindRelatedDocuments {
    fun findRelated(filePath: String, topK: Int = 5): List<RelatedDocResult>
}
