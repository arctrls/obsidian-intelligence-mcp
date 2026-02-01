package com.jazzbach.obsidianintelligence.tagging

import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.annotation.McpToolParam
import org.springframework.stereotype.Component

@Component
class TaggingMcpTool(
    private val tagDocument: TagDocument,
    private val batchTagFolder: BatchTagFolder
) {

    @McpTool(
        name = "tag-document",
        description = "Generate and apply semantic tags to a single Obsidian vault document. " +
                "Analyzes document content using semantic similarity to suggest appropriate hierarchical tags. " +
                "Tags follow the vault's category system: Topic, Document Type, Source, Patterns, Frameworks."
    )
    fun tagDocument(
        @McpToolParam(description = "Absolute path to the markdown file to tag.")
        filePath: String,
        @McpToolParam(description = "If true, only preview generated tags without writing to file. Default is false.", required = false)
        dryRun: Boolean?
    ): TaggingToolResponse {
        val result = tagDocument.tag(filePath, dryRun ?: false)

        return TaggingToolResponse(
            filePath = result.filePath,
            originalTags = result.originalTags,
            generatedTags = result.generatedTags,
            categorizedTags = result.categorizedTags.map { (category, tags) ->
                category.displayName to tags
            }.toMap(),
            success = result.success,
            errorMessage = result.errorMessage,
            dryRun = dryRun ?: false
        )
    }

    @McpTool(
        name = "batch-tag-folder",
        description = "Generate and apply semantic tags to all markdown files in a folder. " +
                "Processes each file using the same semantic tagging system as tag-document. " +
                "Can operate recursively on subfolders."
    )
    fun batchTagFolder(
        @McpToolParam(description = "Absolute path to the folder containing markdown files.")
        folderPath: String,
        @McpToolParam(description = "Whether to process subfolders recursively. Default is true.", required = false)
        recursive: Boolean?,
        @McpToolParam(description = "If true, only preview generated tags without writing to files. Default is false.", required = false)
        dryRun: Boolean?
    ): BatchTaggingToolResponse {
        val results = batchTagFolder.tagFolder(
            folderPath,
            recursive ?: true,
            dryRun ?: false
        )

        val successCount = results.count { it.success }
        val failCount = results.count { !it.success }

        return BatchTaggingToolResponse(
            folderPath = folderPath,
            totalFiles = results.size,
            successCount = successCount,
            failCount = failCount,
            results = results.map { result ->
                TaggingToolResponse(
                    filePath = result.filePath,
                    originalTags = result.originalTags,
                    generatedTags = result.generatedTags,
                    categorizedTags = result.categorizedTags.map { (category, tags) ->
                        category.displayName to tags
                    }.toMap(),
                    success = result.success,
                    errorMessage = result.errorMessage,
                    dryRun = dryRun ?: false
                )
            },
            dryRun = dryRun ?: false
        )
    }
}

data class TaggingToolResponse(
    val filePath: String,
    val originalTags: List<String>,
    val generatedTags: List<String>,
    val categorizedTags: Map<String, List<String>>,
    val success: Boolean,
    val errorMessage: String?,
    val dryRun: Boolean
)

data class BatchTaggingToolResponse(
    val folderPath: String,
    val totalFiles: Int,
    val successCount: Int,
    val failCount: Int,
    val results: List<TaggingToolResponse>,
    val dryRun: Boolean
)
