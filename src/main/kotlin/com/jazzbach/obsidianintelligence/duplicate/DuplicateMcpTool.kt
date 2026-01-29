package com.jazzbach.obsidianintelligence.duplicate

import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service

@Service
class DuplicateMcpTool(
    private val detectDuplicates: DetectDuplicates
) {

    @Tool(
        name = "detect-duplicates",
        description = "Detect duplicate or near-duplicate documents in the Obsidian vault. " +
                "Scans all documents and groups those with high semantic similarity. " +
                "Returns duplicate groups with a master document (highest word count) and its duplicates."
    )
    fun detectDuplicateDocuments(): DuplicateToolResponse {
        val analysis = detectDuplicates.detect()

        return DuplicateToolResponse(
            totalDocumentsScanned = analysis.totalDocumentsScanned,
            duplicateGroupCount = analysis.duplicateGroupCount,
            groups = analysis.groups.map { group ->
                DuplicateGroupResult(
                    master = DuplicateDocumentResult(
                        filePath = group.master.filePath,
                        title = group.master.title,
                        wordCount = group.master.wordCount
                    ),
                    duplicates = group.duplicates.map { dup ->
                        DuplicateDocumentResult(
                            filePath = dup.filePath,
                            title = dup.title,
                            wordCount = dup.wordCount,
                            similarity = "%.4f".format(dup.similarity)
                        )
                    },
                    averageSimilarity = "%.4f".format(group.averageSimilarity)
                )
            }
        )
    }
}

data class DuplicateToolResponse(
    val totalDocumentsScanned: Int,
    val duplicateGroupCount: Int,
    val groups: List<DuplicateGroupResult>
)

data class DuplicateGroupResult(
    val master: DuplicateDocumentResult,
    val duplicates: List<DuplicateDocumentResult>,
    val averageSimilarity: String
)

data class DuplicateDocumentResult(
    val filePath: String,
    val title: String,
    val wordCount: Int,
    val similarity: String = ""
)
