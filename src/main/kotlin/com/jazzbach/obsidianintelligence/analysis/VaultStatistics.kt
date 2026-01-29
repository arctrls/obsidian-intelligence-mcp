package com.jazzbach.obsidianintelligence.analysis

data class VaultStatistics(
    val totalFiles: Int,
    val totalWordCount: Long,
    val averageWordCount: Double,
    val filesByExtension: Map<String, Int>,
    val filesByDirectory: Map<String, Int>,
    val tagAnalysis: TagAnalysis
)

data class TagAnalysis(
    val totalUniqueTags: Int,
    val tagFrequency: Map<String, Int>,
    val topTags: List<Pair<String, Int>>,
    val untaggedFiles: Int,
    val averageTagsPerFile: Double,
    val categoryDistribution: Map<String, Int>
)
