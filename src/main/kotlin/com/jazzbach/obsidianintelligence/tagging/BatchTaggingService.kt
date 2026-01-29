package com.jazzbach.obsidianintelligence.tagging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

@Service
class BatchTaggingService(
    private val tagDocument: TagDocument
) : BatchTagFolder {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun tagFolder(folderPath: String, recursive: Boolean, dryRun: Boolean): List<TaggingResult> {
        val folder = Path.of(folderPath)
        if (!Files.isDirectory(folder)) {
            log.warn("Not a directory: {}", folderPath)
            return emptyList()
        }

        val mdFiles = if (recursive) {
            Files.walk(folder)
                .filter { it.isRegularFile() && it.extension in MARKDOWN_EXTENSIONS }
                .toList()
        } else {
            Files.list(folder)
                .filter { it.isRegularFile() && it.extension in MARKDOWN_EXTENSIONS }
                .toList()
        }

        log.info("Batch tagging {} files in {}", mdFiles.size, folderPath)

        val results = mdFiles.map { file ->
            try {
                tagDocument.tag(file.toString(), dryRun)
            } catch (e: Exception) {
                log.error("Failed to tag {}: {}", file, e.message)
                TaggingResult(
                    filePath = file.toString(),
                    originalTags = emptyList(),
                    generatedTags = emptyList(),
                    categorizedTags = emptyMap(),
                    success = false,
                    errorMessage = e.message
                )
            }
        }

        val successful = results.count { it.success }
        log.info("Batch tagging complete: {}/{} successful", successful, mdFiles.size)

        return results
    }

    companion object {
        private val MARKDOWN_EXTENSIONS = setOf("md", "markdown")
    }
}
