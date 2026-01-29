package com.jazzbach.obsidianintelligence.tagging

import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.shared.MarkdownWriter
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document as AiDocument
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class SemanticTaggingService(
    private val documentParser: DocumentParser,
    private val vectorStore: VectorStore,
    private val tagRuleEngine: TagRuleEngine,
    private val markdownWriter: MarkdownWriter,
    private val taggingProperties: TaggingProperties
) : TagDocument {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun tag(filePath: String, dryRun: Boolean): TaggingResult {
        return try {
            val document = documentParser.parse(Path.of(filePath))
            val originalTags = document.tags

            // Find similar documents to derive tags
            val candidateTags = mutableListOf<String>()

            // 1. Search for similar documents in vector store
            val similarDocs = findSimilarDocuments(document.cleanedContent)
            for (doc in similarDocs) {
                val tags = (doc.metadata["tags"]?.toString() ?: "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                candidateTags.addAll(tags)
            }

            // 2. Pattern-based tag generation from filename and headers
            candidateTags.addAll(generatePatternTags(filePath, document.content))

            // 3. Topic distribution based tags
            candidateTags.addAll(generateTopicTags(document.content))

            // 4. Normalize and validate
            val validatedTags = candidateTags
                .map { tagRuleEngine.normalizeTag(it) }
                .filter { tagRuleEngine.validateTag(it) }
                .distinct()

            // 5. Apply category-based limits
            val finalTags = tagRuleEngine.limitTagCount(
                validatedTags,
                taggingProperties.maxTagsPerDocument
            )

            val categorizedTags = tagRuleEngine.categorizeTags(finalTags)

            // Apply tags to file if not dry run
            if (!dryRun && finalTags.isNotEmpty()) {
                markdownWriter.updateFrontmatter(
                    Path.of(filePath),
                    mapOf("tags" to finalTags)
                )
            }

            TaggingResult(
                filePath = filePath,
                originalTags = originalTags,
                generatedTags = finalTags,
                categorizedTags = categorizedTags,
                success = true
            )
        } catch (e: Exception) {
            log.error("Failed to tag document {}: {}", filePath, e.message, e)
            TaggingResult(
                filePath = filePath,
                originalTags = emptyList(),
                generatedTags = emptyList(),
                categorizedTags = emptyMap(),
                success = false,
                errorMessage = e.message
            )
        }
    }

    private fun findSimilarDocuments(content: String): List<AiDocument> {
        return try {
            val request = SearchRequest.builder()
                .query(content.take(500))
                .topK(5)
                .similarityThreshold(taggingProperties.minSemanticSimilarity)
                .build()
            vectorStore.similaritySearch(request)
        } catch (e: Exception) {
            log.debug("Similar document search failed: {}", e.message)
            emptyList()
        }
    }

    private fun generatePatternTags(filePath: String, content: String): List<String> {
        val tags = mutableListOf<String>()
        val fileName = Path.of(filePath).fileName.toString().lowercase()

        if ("spring" in fileName) tags.add("frameworks/spring-boot")
        if ("tdd" in fileName || "test" in fileName) tags.add("testing/tdd")
        if ("clean" in fileName && "code" in fileName) tags.add("practices/clean-code")

        // Extract from headers
        val headers = HEADER_PATTERN.findAll(content)
            .map { it.groupValues[1].lowercase() }
            .joinToString(" ")

        if ("architecture" in headers) tags.add("architecture/design")
        if ("refactor" in headers) tags.add("practices/refactoring")
        if ("pattern" in headers) tags.add("patterns/design-patterns")

        return tags
    }

    private fun generateTopicTags(content: String): List<String> {
        val tags = mutableListOf<String>()
        val contentLower = content.lowercase()

        for ((topic, keywords) in TOPIC_KEYWORDS) {
            val score = keywords.count { it in contentLower }.toDouble() / keywords.size
            if (score > 0.3) {
                tags.add("$topic/general")
            }
        }

        return tags
    }

    companion object {
        private val HEADER_PATTERN = Regex("(?m)^#+\\s+(.+)$")

        private val TOPIC_KEYWORDS = mapOf(
            "architecture" to listOf("architecture", "design", "pattern", "structure"),
            "development" to listOf("development", "coding", "programming", "implementation"),
            "testing" to listOf("test", "testing", "tdd", "unit", "integration"),
            "framework" to listOf("spring", "react", "vue", "angular", "framework"),
            "database" to listOf("database", "sql", "query", "data", "storage")
        )
    }
}
