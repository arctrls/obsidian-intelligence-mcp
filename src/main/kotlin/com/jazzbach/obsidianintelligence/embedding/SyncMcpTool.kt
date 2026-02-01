package com.jazzbach.obsidianintelligence.embedding

import org.springaicommunity.mcp.annotation.McpTool
import org.springframework.stereotype.Component

@Component
class SyncMcpTool(
    private val documentEmbeddingService: DocumentEmbeddingService
) {

    @McpTool(
        name = "sync-vault",
        description = "Synchronize the Obsidian vault with the vector store. " +
                "Scans all markdown files, generates embeddings using BGE-M3, " +
                "and stores them in PgVector for semantic search. " +
                "Must be run at least once before using search, tagging, " +
                "related documents, duplicate detection, or topic collection tools."
    )
    fun syncVault(): SyncVaultToolResponse {
        val result = documentEmbeddingService.syncVault()

        return SyncVaultToolResponse(
            totalFiles = result.totalFiles,
            processedFiles = result.processedFiles,
            skippedFiles = result.skippedFiles,
            failedFiles = result.failedFiles
        )
    }
}

data class SyncVaultToolResponse(
    val totalFiles: Int,
    val processedFiles: Int,
    val skippedFiles: Int,
    val failedFiles: Int
)
