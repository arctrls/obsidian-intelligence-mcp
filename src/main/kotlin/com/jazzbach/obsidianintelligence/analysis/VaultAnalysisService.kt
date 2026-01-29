package com.jazzbach.obsidianintelligence.analysis

import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.tagging.TagRuleEngine
import com.jazzbach.obsidianintelligence.vault.VaultScanner
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.io.path.extension

@Service
class VaultAnalysisService(
    private val vaultScanner: VaultScanner,
    private val documentParser: DocumentParser,
    private val tagRuleEngine: TagRuleEngine
) : AnalyzeVault {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun analyze(): VaultStatistics {
        val files = vaultScanner.findAllFiles()
        log.info("Analyzing vault with {} files", files.size)

        val filesByExtension = mutableMapOf<String, Int>()
        val filesByDirectory = mutableMapOf<String, Int>()
        val tagCounter = mutableMapOf<String, Int>()
        var totalWordCount = 0L
        var untaggedFiles = 0
        var totalTagCount = 0

        for (file in files) {
            // Extension stats
            val ext = file.extension
            filesByExtension[ext] = (filesByExtension[ext] ?: 0) + 1

            // Directory stats
            val dir = file.parent?.fileName?.toString() ?: "root"
            filesByDirectory[dir] = (filesByDirectory[dir] ?: 0) + 1

            try {
                val doc = documentParser.parse(file)
                totalWordCount += doc.wordCount

                if (doc.tags.isEmpty()) {
                    untaggedFiles++
                } else {
                    totalTagCount += doc.tags.size
                    for (tag in doc.tags) {
                        val normalized = tagRuleEngine.normalizeTag(tag)
                        tagCounter[normalized] = (tagCounter[normalized] ?: 0) + 1
                    }
                }
            } catch (e: Exception) {
                log.warn("Failed to parse {} for analysis: {}", file, e.message)
            }
        }

        val totalFiles = files.size
        val averageWordCount = if (totalFiles > 0) totalWordCount.toDouble() / totalFiles else 0.0
        val averageTagsPerFile = if (totalFiles > 0) totalTagCount.toDouble() / totalFiles else 0.0

        val topTags = tagCounter.entries
            .sortedByDescending { it.value }
            .take(20)
            .map { it.key to it.value }

        // Category distribution
        val allTags = tagCounter.keys.toList()
        val categorized = tagRuleEngine.categorizeTags(allTags)
        val categoryDistribution = categorized.map { (category, tags) ->
            category.displayName to tags.size
        }.toMap()

        val tagAnalysis = TagAnalysis(
            totalUniqueTags = tagCounter.size,
            tagFrequency = tagCounter,
            topTags = topTags,
            untaggedFiles = untaggedFiles,
            averageTagsPerFile = averageTagsPerFile,
            categoryDistribution = categoryDistribution
        )

        return VaultStatistics(
            totalFiles = totalFiles,
            totalWordCount = totalWordCount,
            averageWordCount = averageWordCount,
            filesByExtension = filesByExtension,
            filesByDirectory = filesByDirectory,
            tagAnalysis = tagAnalysis
        )
    }
}
