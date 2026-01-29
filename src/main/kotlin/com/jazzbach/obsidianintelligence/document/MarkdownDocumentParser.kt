package com.jazzbach.obsidianintelligence.document

import com.jazzbach.obsidianintelligence.shared.FileHashCalculator
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class MarkdownDocumentParser(
    private val frontmatterParser: FrontmatterParser,
    private val contentCleaner: ContentCleaner,
    private val fileHashCalculator: FileHashCalculator
) : DocumentParser {

    override fun parse(path: Path): Document {
        val content = Files.readString(path)
        val frontmatter = frontmatterParser.parse(content)
        val body = frontmatterParser.extractBody(content)
        val cleanedContent = contentCleaner.clean(content)
        val title = extractTitle(frontmatter, body, path)
        val tags = collectTags(frontmatter, body)
        val wordCount = countWords(cleanedContent)
        val fileHash = fileHashCalculator.calculate(path)
        val modifiedAt = Files.getLastModifiedTime(path).toInstant()

        return Document(
            path = path,
            title = title,
            content = content,
            cleanedContent = cleanedContent,
            frontmatter = frontmatter,
            tags = tags,
            wordCount = wordCount,
            fileHash = fileHash,
            modifiedAt = modifiedAt
        )
    }

    private fun extractTitle(frontmatter: Frontmatter, body: String, path: Path): String {
        // Priority: frontmatter title > first H1 header > filename
        frontmatter.title?.let { return it }

        H1_PATTERN.find(body)?.let { return it.groupValues[1].trim() }

        return path.fileName.toString().removeSuffix(".md").removeSuffix(".markdown")
    }

    private fun collectTags(frontmatter: Frontmatter, body: String): List<String> {
        val tags = mutableSetOf<String>()
        tags.addAll(frontmatter.tags)

        INLINE_TAG_PATTERN.findAll(body).forEach { match ->
            val tag = match.value.removePrefix("#")
            tags.add(tag)
        }

        return tags.toList()
    }

    private fun countWords(cleanedContent: String): Int {
        if (cleanedContent.isBlank()) return 0

        var count = 0
        val koreanPattern = KOREAN_CHAR_PATTERN

        for (line in cleanedContent.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Count Korean characters individually (no space-delimited words)
            val koreanChars = koreanPattern.findAll(trimmed).count()

            // Count non-Korean words (space-delimited)
            val nonKorean = koreanPattern.replace(trimmed, " ").trim()
            val nonKoreanWords = if (nonKorean.isBlank()) 0
            else nonKorean.split(WHITESPACE_PATTERN).filter { it.isNotBlank() }.size

            count += koreanChars + nonKoreanWords
        }

        return count
    }

    companion object {
        private val H1_PATTERN = Regex("(?m)^#\\s+(.*)")
        private val INLINE_TAG_PATTERN = Regex("(?<=\\s|^)#([a-zA-Z][\\w/-]*)")
        private val KOREAN_CHAR_PATTERN = Regex("[가-힣]")
        private val WHITESPACE_PATTERN = Regex("\\s+")
    }
}
