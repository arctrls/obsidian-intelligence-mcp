package com.jazzbach.obsidianintelligence.expansion

interface ExpandQuery {
    fun expand(query: String): ExpandedQuery
}
