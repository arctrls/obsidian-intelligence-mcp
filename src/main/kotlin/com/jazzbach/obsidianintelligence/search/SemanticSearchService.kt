package com.jazzbach.obsidianintelligence.search

import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class SemanticSearchService(
    private val vectorStore: VectorStore,
    private val searchProperties: SearchProperties,
    private val keywordSearchService: KeywordSearchService
) : SearchDocuments {

    override fun search(query: SearchQuery): List<SearchResult> {
        return when (query.searchType) {
            SearchType.SEMANTIC -> semanticSearch(query)
            SearchType.KEYWORD -> keywordSearch(query)
            SearchType.HYBRID -> hybridSearch(query)
        }
    }

    private fun semanticSearch(query: SearchQuery): List<SearchResult> {
        val topK = if (query.topK > 0) query.topK else searchProperties.defaultTopK
        val threshold = if (query.similarityThreshold > 0) query.similarityThreshold
        else searchProperties.similarityThreshold

        val searchRequest = SearchRequest.builder()
            .query(query.text)
            .topK(topK)
            .similarityThreshold(threshold)
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)

        return documents
            .filter { doc -> passesFilters(doc, query) }
            .map { doc -> toSearchResult(doc, query) }
    }

    private fun keywordSearch(query: SearchQuery): List<SearchResult> {
        val topK = if (query.topK > 0) query.topK else searchProperties.defaultTopK
        val threshold = if (query.similarityThreshold > 0) query.similarityThreshold
        else searchProperties.similarityThreshold

        val searchRequest = SearchRequest.builder()
            .query(query.text)
            .topK(topK * 2)
            .similarityThreshold(threshold)
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)
        val keywords = query.text.lowercase().split(WHITESPACE_PATTERN).filter { it.length > 1 }

        return documents
            .filter { doc -> passesFilters(doc, query) }
            .map { doc ->
                val content = doc.text ?: ""
                val title = doc.metadata["title"]?.toString() ?: ""
                val tags = parseTags(doc.metadata["tags"]?.toString() ?: "")

                val keywordScore = keywordSearchService.calculateScore(title, tags, content, keywords)

                SearchResult(
                    filePath = doc.metadata["filePath"]?.toString() ?: "",
                    title = title,
                    score = keywordScore,
                    snippet = generateSnippet(content, query.text),
                    tags = tags,
                    wordCount = (doc.metadata["wordCount"] as? Number)?.toInt() ?: 0
                )
            }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun hybridSearch(query: SearchQuery): List<SearchResult> {
        val topK = if (query.topK > 0) query.topK else searchProperties.defaultTopK
        val threshold = if (query.similarityThreshold > 0) query.similarityThreshold
        else searchProperties.similarityThreshold

        val searchRequest = SearchRequest.builder()
            .query(query.text)
            .topK(topK * 2)
            .similarityThreshold(threshold)
            .build()

        val documents = vectorStore.similaritySearch(searchRequest)
        val keywords = query.text.lowercase().split(WHITESPACE_PATTERN).filter { it.length > 1 }

        val filtered = documents.filter { doc -> passesFilters(doc, query) }

        val keywordScores = filtered.map { doc ->
            val content = doc.text ?: ""
            val title = doc.metadata["title"]?.toString() ?: ""
            val tags = parseTags(doc.metadata["tags"]?.toString() ?: "")
            keywordSearchService.calculateScore(title, tags, content, keywords)
        }

        val maxKeywordScore = keywordScores.maxOrNull() ?: 1.0
        val normalizedKeywordScores = if (maxKeywordScore > 0) {
            keywordScores.map { it / maxKeywordScore }
        } else {
            keywordScores.map { 0.0 }
        }

        val denseWeight = searchProperties.hybridDenseWeight
        val keywordWeight = searchProperties.hybridKeywordWeight

        return filtered.zip(normalizedKeywordScores)
            .map { (doc, normalizedKeyword) ->
                val denseScore = doc.score ?: 0.0
                val combinedScore = denseScore * denseWeight + normalizedKeyword * keywordWeight
                val content = doc.text ?: ""

                SearchResult(
                    filePath = doc.metadata["filePath"]?.toString() ?: "",
                    title = doc.metadata["title"]?.toString() ?: "",
                    score = combinedScore,
                    snippet = generateSnippet(content, query.text),
                    tags = parseTags(doc.metadata["tags"]?.toString() ?: ""),
                    wordCount = (doc.metadata["wordCount"] as? Number)?.toInt() ?: 0
                )
            }
            .sortedByDescending { it.score }
            .take(topK)
    }

    private fun passesFilters(
        doc: org.springframework.ai.document.Document,
        query: SearchQuery
    ): Boolean {
        val filePath = doc.metadata["filePath"]?.toString() ?: ""
        if (query.excludePaths.any { excluded -> filePath.contains(excluded) }) return false

        if (query.tags.isNotEmpty()) {
            val docTags = parseTags(doc.metadata["tags"]?.toString() ?: "")
            if (query.tags.none { queryTag -> docTags.any { it.contains(queryTag, ignoreCase = true) } }) {
                return false
            }
        }
        return true
    }

    private fun toSearchResult(
        doc: org.springframework.ai.document.Document,
        query: SearchQuery
    ): SearchResult {
        val content = doc.text ?: ""
        return SearchResult(
            filePath = doc.metadata["filePath"]?.toString() ?: "",
            title = doc.metadata["title"]?.toString() ?: "",
            score = doc.score ?: 0.0,
            snippet = generateSnippet(content, query.text),
            tags = parseTags(doc.metadata["tags"]?.toString() ?: ""),
            wordCount = (doc.metadata["wordCount"] as? Number)?.toInt() ?: 0
        )
    }

    private fun parseTags(tagsString: String): List<String> {
        return tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun generateSnippet(content: String, queryText: String, maxLength: Int = 200): String {
        if (content.isBlank()) return ""

        val queryWords = queryText.lowercase().split(WHITESPACE_PATTERN).filter { it.length > 2 }
        val sentences = content.split(SENTENCE_PATTERN).filter { it.isNotBlank() }

        if (sentences.isEmpty()) return content.take(maxLength)

        val bestSentence = sentences.maxByOrNull { sentence ->
            val lowerSentence = sentence.lowercase()
            queryWords.count { word -> lowerSentence.contains(word) }
        } ?: sentences.first()

        return if (bestSentence.length > maxLength) {
            bestSentence.take(maxLength - 3) + "..."
        } else {
            bestSentence.trim()
        }
    }

    companion object {
        private val WHITESPACE_PATTERN = Regex("\\s+")
        private val SENTENCE_PATTERN = Regex("[.!?\\n]+")
    }
}
