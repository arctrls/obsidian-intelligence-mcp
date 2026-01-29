package com.jazzbach.obsidianintelligence.related

import com.jazzbach.obsidianintelligence.document.DocumentParser
import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class RelatedDocsService(
    private val documentParser: DocumentParser,
    private val vectorStore: VectorStore,
    private val relatedDocsProperties: RelatedDocsProperties
) : FindRelatedDocuments {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun findRelated(filePath: String, topK: Int): List<RelatedDocResult> {
        return try {
            val document = documentParser.parse(Path.of(filePath))
            val effectiveTopK = if (topK > 0) topK else relatedDocsProperties.defaultTopK

            // Search for similar documents, requesting extra to account for self-match
            val searchRequest = SearchRequest.builder()
                .query(document.cleanedContent.take(500))
                .topK(effectiveTopK + 1)
                .similarityThreshold(relatedDocsProperties.similarityThreshold)
                .build()

            val results = vectorStore.similaritySearch(searchRequest)

            results
                .filter { doc ->
                    // Exclude the document itself
                    val docPath = doc.metadata["filePath"]?.toString() ?: ""
                    docPath != filePath
                }
                .take(effectiveTopK)
                .map { doc ->
                    RelatedDocResult(
                        filePath = doc.metadata["filePath"]?.toString() ?: "",
                        title = doc.metadata["title"]?.toString() ?: "",
                        score = doc.score ?: 0.0,
                        tags = (doc.metadata["tags"]?.toString() ?: "")
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    )
                }
        } catch (e: Exception) {
            log.error("Failed to find related documents for {}: {}", filePath, e.message, e)
            emptyList()
        }
    }
}
