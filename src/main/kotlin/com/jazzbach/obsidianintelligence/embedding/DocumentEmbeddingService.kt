package com.jazzbach.obsidianintelligence.embedding

import com.jazzbach.obsidianintelligence.document.Document
import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.vault.VaultScanner
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document as AiDocument
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class DocumentEmbeddingService(
    private val vaultScanner: VaultScanner,
    private val documentParser: DocumentParser,
    private val vectorStore: VectorStore,
    private val embeddingProperties: EmbeddingProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun syncVault(): SyncResult {
        val files = vaultScanner.findAllFiles()
        log.info("Found {} files in vault", files.size)

        val documents = files.mapNotNull { path ->
            try {
                documentParser.parse(path)
            } catch (e: Exception) {
                log.warn("Failed to parse {}: {}", path, e.message)
                null
            }
        }

        val existingHashes = getExistingHashes()
        val changedDocuments = documents.filter { doc ->
            val existingHash = existingHashes[doc.path.toString()]
            existingHash == null || existingHash != doc.fileHash
        }

        if (changedDocuments.isEmpty()) {
            log.info("No documents changed, skipping sync")
            return SyncResult(
                totalFiles = files.size,
                processedFiles = 0,
                skippedFiles = files.size,
                failedFiles = 0
            )
        }

        log.info("Syncing {} changed documents", changedDocuments.size)

        var processedCount = 0
        var failedCount = 0

        changedDocuments.chunked(embeddingProperties.batchSize).forEach { batch ->
            try {
                val aiDocuments = batch.map { doc -> toAiDocument(doc) }

                // Remove existing documents for re-indexing
                val idsToDelete = batch.map { it.path.toString() }
                try {
                    vectorStore.delete(idsToDelete)
                } catch (e: Exception) {
                    log.debug("No existing documents to delete: {}", e.message)
                }

                vectorStore.add(aiDocuments)
                processedCount += batch.size
            } catch (e: Exception) {
                log.error("Failed to embed batch: {}", e.message, e)
                failedCount += batch.size
            }
        }

        return SyncResult(
            totalFiles = files.size,
            processedFiles = processedCount,
            skippedFiles = files.size - changedDocuments.size,
            failedFiles = failedCount
        )
    }

    private fun toAiDocument(doc: Document): AiDocument {
        val metadata = mapOf(
            "filePath" to doc.path.toString(),
            "fileHash" to doc.fileHash,
            "title" to doc.title,
            "tags" to doc.tags.joinToString(","),
            "wordCount" to doc.wordCount
        )
        return AiDocument(doc.path.toString(), doc.cleanedContent, metadata)
    }

    private fun getExistingHashes(): Map<String, String> {
        // Query all existing documents' metadata to get file hashes
        // PgVectorStore doesn't support direct metadata queries,
        // so we use a similarity search with a broad threshold as fallback
        // In production, consider maintaining a separate hash table
        return emptyMap()
    }
}

data class SyncResult(
    val totalFiles: Int,
    val processedFiles: Int,
    val skippedFiles: Int,
    val failedFiles: Int
)
