package com.jazzbach.obsidianintelligence.duplicate

import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.vault.VaultScanner
import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class DuplicateDetectionService(
    private val vaultScanner: VaultScanner,
    private val documentParser: DocumentParser,
    private val vectorStore: VectorStore,
    private val duplicateProperties: DuplicateProperties
) : DetectDuplicates {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun detect(): DuplicateAnalysis {
        val files = vaultScanner.findAllFiles()
        log.info("Scanning {} files for duplicates", files.size)

        val documents = files.mapNotNull { path ->
            try {
                val doc = documentParser.parse(path)
                if (doc.wordCount >= duplicateProperties.minWordCount) doc else null
            } catch (e: Exception) {
                log.warn("Failed to parse {} for duplicate detection: {}", path, e.message)
                null
            }
        }

        log.info("Filtered to {} documents with minWordCount >= {}", documents.size, duplicateProperties.minWordCount)

        val visited = mutableSetOf<String>()
        val groups = mutableListOf<DuplicateGroup>()

        for (doc in documents) {
            val filePath = doc.path.toString()
            if (filePath in visited) continue

            val searchRequest = SearchRequest.builder()
                .query(doc.cleanedContent.take(1000))
                .topK(20)
                .similarityThreshold(duplicateProperties.similarityThreshold)
                .build()

            val similar = try {
                vectorStore.similaritySearch(searchRequest)
            } catch (e: Exception) {
                log.warn("Similarity search failed for {}: {}", filePath, e.message)
                continue
            }

            val matchedDocs = similar
                .filter { aiDoc ->
                    val aiFilePath = aiDoc.metadata["filePath"]?.toString() ?: ""
                    aiFilePath != filePath && aiFilePath !in visited
                }
                .map { aiDoc ->
                    DuplicateDocument(
                        filePath = aiDoc.metadata["filePath"]?.toString() ?: "",
                        title = aiDoc.metadata["title"]?.toString() ?: "",
                        wordCount = (aiDoc.metadata["wordCount"] as? Number)?.toInt() ?: 0,
                        similarity = aiDoc.score ?: 0.0
                    )
                }

            if (matchedDocs.isNotEmpty()) {
                val allInGroup = listOf(
                    DuplicateDocument(
                        filePath = filePath,
                        title = doc.title,
                        wordCount = doc.wordCount,
                        similarity = 1.0
                    )
                ) + matchedDocs

                val master = allInGroup.maxBy { it.wordCount }
                val duplicates = allInGroup.filter { it.filePath != master.filePath }
                val avgSimilarity = duplicates.map { it.similarity }.average()

                groups.add(
                    DuplicateGroup(
                        master = master,
                        duplicates = duplicates,
                        averageSimilarity = avgSimilarity
                    )
                )

                visited.add(filePath)
                matchedDocs.forEach { visited.add(it.filePath) }
            }
        }

        log.info("Found {} duplicate groups", groups.size)

        return DuplicateAnalysis(
            totalDocumentsScanned = documents.size,
            duplicateGroupCount = groups.size,
            groups = groups
        )
    }
}
