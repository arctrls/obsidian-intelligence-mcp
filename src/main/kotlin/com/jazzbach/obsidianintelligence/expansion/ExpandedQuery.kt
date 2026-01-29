package com.jazzbach.obsidianintelligence.expansion

data class ExpandedQuery(
    val originalQuery: String,
    val expandedTerms: List<String>,
    val hydeDocument: String,
    val allTerms: List<String>
) {
    fun combinedQuery(): String {
        val terms = (listOf(originalQuery) + expandedTerms).distinct()
        return terms.joinToString(" ")
    }
}
