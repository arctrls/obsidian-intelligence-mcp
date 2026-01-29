package com.jazzbach.obsidianintelligence.duplicate

data class DuplicateAnalysis(
    val totalDocumentsScanned: Int,
    val duplicateGroupCount: Int,
    val groups: List<DuplicateGroup>
)

data class DuplicateGroup(
    val master: DuplicateDocument,
    val duplicates: List<DuplicateDocument>,
    val averageSimilarity: Double
)

data class DuplicateDocument(
    val filePath: String,
    val title: String,
    val wordCount: Int,
    val similarity: Double
)
