package com.jazzbach.obsidianintelligence.search

import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class SemanticSearchService(
    private val vectorStore: VectorStore,
    private val searchProperties: SearchProperties
) : SearchDocuments {

    override fun search(query: SearchQuery): List<SearchResult> {
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
            .filter { doc ->
                val filePath = doc.metadata["filePath"]?.toString() ?: ""
                query.excludePaths.none { excluded -> filePath.contains(excluded) }
            }
            .filter { doc ->
                if (query.tags.isEmpty()) true
                else {
                    val docTags = (doc.metadata["tags"]?.toString() ?: "")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    query.tags.any { queryTag -> docTags.any { it.contains(queryTag, ignoreCase = true) } }
                }
            }
            .map { doc ->
                val content = doc.text ?: ""
                SearchResult(
                    filePath = doc.metadata["filePath"]?.toString() ?: "",
                    title = doc.metadata["title"]?.toString() ?: "",
                    score = doc.score ?: 0.0,
                    snippet = generateSnippet(content, query.text),
                    tags = (doc.metadata["tags"]?.toString() ?: "")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                    wordCount = (doc.metadata["wordCount"] as? Number)?.toInt() ?: 0
                )
            }
    }

    private fun generateSnippet(content: String, queryText: String, maxLength: Int = 200): String {
        if (content.isBlank()) return ""

        val queryWords = queryText.lowercase().split(WHITESPACE_PATTERN).filter { it.length > 2 }
        val sentences = content.split(SENTENCE_PATTERN).filter { it.isNotBlank() }

        if (sentences.isEmpty()) return content.take(maxLength)

        // Find sentence with most keyword matches
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
