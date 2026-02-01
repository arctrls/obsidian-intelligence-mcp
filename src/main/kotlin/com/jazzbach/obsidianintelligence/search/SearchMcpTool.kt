package com.jazzbach.obsidianintelligence.search

import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class SearchMcpTool(
    private val searchDocuments: SearchDocuments,
    private val searchProperties: SearchProperties
) {

    @McpTool(
        name = "search-documents",
        description = "Search Obsidian vault documents using semantic similarity, keyword matching, or hybrid approach. " +
                "Returns the most relevant documents matching the query text. " +
                "Supports filtering by tags and excluding specific paths."
    )
    fun searchDocuments(
        @McpToolParam(description = "The search query text. Can be a question, keyword, or natural language description.")
        query: String,
        @McpToolParam(description = "Maximum number of results to return. Default is 10.", required = false)
        topK: Int?,
        @McpToolParam(description = "Minimum similarity threshold (0.0 to 1.0). Default is 0.3.", required = false)
        similarityThreshold: Double?,
        @McpToolParam(description = "Comma-separated list of tags to filter results by.", required = false)
        tags: String?,
        @McpToolParam(description = "Comma-separated list of path substrings to exclude from results.", required = false)
        excludePaths: String?,
        @McpToolParam(description = "Search type: SEMANTIC (default), KEYWORD, or HYBRID.", required = false)
        searchType: String?
    ): SearchToolResponse {
        val resolvedSearchType = searchType?.let {
            try { SearchType.valueOf(it.uppercase()) } catch (_: IllegalArgumentException) { SearchType.SEMANTIC }
        } ?: SearchType.SEMANTIC

        val searchQuery = SearchQuery(
            text = query,
            topK = topK ?: searchProperties.defaultTopK,
            similarityThreshold = similarityThreshold ?: searchProperties.similarityThreshold,
            tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            excludePaths = excludePaths?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            searchType = resolvedSearchType
        )

        val results = searchDocuments.search(searchQuery)

        return SearchToolResponse(
            query = query,
            resultCount = results.size,
            results = results.map { result ->
                SearchToolResult(
                    filePath = result.filePath,
                    title = result.title,
                    score = "%.4f".format(result.score),
                    snippet = result.snippet,
                    tags = result.tags,
                    wordCount = result.wordCount
                )
            }
        )
    }
}

data class SearchToolResponse(
    val query: String,
    val resultCount: Int,
    val results: List<SearchToolResult>
)

data class SearchToolResult(
    val filePath: String,
    val title: String,
    val score: String,
    val snippet: String,
    val tags: List<String>,
    val wordCount: Int
)
