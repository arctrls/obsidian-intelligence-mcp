package com.jazzbach.obsidianintelligence.related

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service

@Service
class RelatedMcpTool(
    private val findRelatedDocuments: FindRelatedDocuments,
    private val updateRelatedSection: UpdateRelatedSection,
    private val relatedDocsProperties: RelatedDocsProperties
) {

    @Tool(
        name = "find-related-documents",
        description = "Find documents semantically related to a given Obsidian vault document. " +
                "Returns a ranked list of similar documents with similarity scores and tags."
    )
    fun findRelatedDocuments(
        @ToolParam(description = "Absolute path to the target markdown file.")
        filePath: String,
        @ToolParam(description = "Maximum number of related documents to return. Default is 5.", required = false)
        topK: Int?
    ): RelatedDocsToolResponse {
        val results = findRelatedDocuments.findRelated(
            filePath,
            topK ?: relatedDocsProperties.defaultTopK
        )

        return RelatedDocsToolResponse(
            targetFilePath = filePath,
            relatedDocuments = results.map { doc ->
                RelatedDocToolResult(
                    filePath = doc.filePath,
                    title = doc.title,
                    score = "%.4f".format(doc.score),
                    tags = doc.tags
                )
            },
            resultCount = results.size
        )
    }

    @Tool(
        name = "update-related-section",
        description = "Add or update a '관련 문서' (Related Documents) section in an Obsidian vault document. " +
                "Finds semantically related documents and writes wiki-links with similarity scores."
    )
    fun updateRelatedSection(
        @ToolParam(description = "Absolute path to the target markdown file.")
        filePath: String,
        @ToolParam(description = "Maximum number of related documents to include. Default is 5.", required = false)
        topK: Int?
    ): RelatedSectionToolResponse {
        val result = updateRelatedSection.update(
            filePath,
            topK ?: relatedDocsProperties.defaultTopK
        )

        return RelatedSectionToolResponse(
            filePath = result.filePath,
            sectionAdded = result.sectionAdded,
            relatedDocsCount = result.relatedDocs.size,
            success = result.success,
            errorMessage = result.errorMessage
        )
    }
}

data class RelatedDocsToolResponse(
    val targetFilePath: String,
    val relatedDocuments: List<RelatedDocToolResult>,
    val resultCount: Int
)

data class RelatedDocToolResult(
    val filePath: String,
    val title: String,
    val score: String,
    val tags: List<String>
)

data class RelatedSectionToolResponse(
    val filePath: String,
    val sectionAdded: Boolean,
    val relatedDocsCount: Int,
    val success: Boolean,
    val errorMessage: String?
)
