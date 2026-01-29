package com.jazzbach.obsidianintelligence.search

interface SearchDocuments {
    fun search(query: SearchQuery): List<SearchResult>
}
