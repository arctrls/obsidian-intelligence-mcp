package com.jazzbach.obsidianintelligence.analysis

import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Component

@Component
class AnalysisMcpTool(
    private val analyzeVault: AnalyzeVault
) {

    @Tool(
        name = "analyze-vault",
        description = "Analyze the Obsidian vault to get comprehensive statistics. " +
                "Returns file counts, word counts, tag frequency analysis, category distribution, " +
                "and identifies untagged files. Useful for understanding vault structure and content coverage."
    )
    fun analyzeVault(): VaultAnalysisToolResponse {
        val stats = analyzeVault.analyze()

        return VaultAnalysisToolResponse(
            totalFiles = stats.totalFiles,
            totalWordCount = stats.totalWordCount,
            averageWordCount = "%.1f".format(stats.averageWordCount),
            filesByExtension = stats.filesByExtension,
            filesByDirectory = stats.filesByDirectory.entries
                .sortedByDescending { it.value }
                .take(20)
                .associate { it.key to it.value },
            tagAnalysis = TagAnalysisResponse(
                totalUniqueTags = stats.tagAnalysis.totalUniqueTags,
                topTags = stats.tagAnalysis.topTags.map { (tag, count) ->
                    TagFrequencyItem(tag = tag, count = count)
                },
                untaggedFiles = stats.tagAnalysis.untaggedFiles,
                averageTagsPerFile = "%.1f".format(stats.tagAnalysis.averageTagsPerFile),
                categoryDistribution = stats.tagAnalysis.categoryDistribution
            )
        )
    }
}

data class VaultAnalysisToolResponse(
    val totalFiles: Int,
    val totalWordCount: Long,
    val averageWordCount: String,
    val filesByExtension: Map<String, Int>,
    val filesByDirectory: Map<String, Int>,
    val tagAnalysis: TagAnalysisResponse
)

data class TagAnalysisResponse(
    val totalUniqueTags: Int,
    val topTags: List<TagFrequencyItem>,
    val untaggedFiles: Int,
    val averageTagsPerFile: String,
    val categoryDistribution: Map<String, Int>
)

data class TagFrequencyItem(
    val tag: String,
    val count: Int
)
