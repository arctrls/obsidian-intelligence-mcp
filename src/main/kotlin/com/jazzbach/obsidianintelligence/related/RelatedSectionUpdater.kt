package com.jazzbach.obsidianintelligence.related

import com.jazzbach.obsidianintelligence.shared.MarkdownWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class RelatedSectionUpdater(
    private val findRelatedDocuments: FindRelatedDocuments,
    private val markdownWriter: MarkdownWriter,
    private val relatedDocsProperties: RelatedDocsProperties
) : UpdateRelatedSection {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun update(filePath: String, topK: Int): RelatedSectionResult {
        return try {
            val relatedDocs = findRelatedDocuments.findRelated(filePath, topK)

            if (relatedDocs.isEmpty()) {
                return RelatedSectionResult(
                    filePath = filePath,
                    relatedDocs = emptyList(),
                    sectionAdded = false,
                    success = true
                )
            }

            val sectionContent = formatRelatedSection(relatedDocs, filePath)
            val path = Path.of(filePath)
            val content = Files.readString(path)
            val updatedContent = markdownWriter.updateSection(
                content,
                relatedDocsProperties.sectionTitle,
                sectionContent
            )
            Files.writeString(path, updatedContent)

            RelatedSectionResult(
                filePath = filePath,
                relatedDocs = relatedDocs,
                sectionAdded = true,
                success = true
            )
        } catch (e: Exception) {
            log.error("Failed to update related section for {}: {}", filePath, e.message, e)
            RelatedSectionResult(
                filePath = filePath,
                relatedDocs = emptyList(),
                sectionAdded = false,
                success = false,
                errorMessage = e.message
            )
        }
    }

    private fun formatRelatedSection(relatedDocs: List<RelatedDocResult>, sourcePath: String): String {
        val vaultRoot = findVaultRoot(sourcePath)
        return relatedDocs.joinToString("\n") { doc ->
            val displayTitle = doc.title.ifBlank {
                Path.of(doc.filePath).fileName.toString().removeSuffix(".md")
            }
            val relativePath = if (vaultRoot != null) {
                Path.of(vaultRoot).relativize(Path.of(doc.filePath)).toString()
                    .removeSuffix(".md").removeSuffix(".markdown")
            } else {
                displayTitle
            }
            "- [[${relativePath}|${displayTitle}]] (유사도: ${"%.2f".format(doc.score)})"
        } + "\n"
    }

    private fun findVaultRoot(filePath: String): String? {
        var current = Path.of(filePath).parent
        while (current != null) {
            if (Files.isDirectory(current.resolve(".obsidian"))) {
                return current.toString()
            }
            current = current.parent
        }
        return null
    }
}
