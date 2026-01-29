package com.jazzbach.obsidianintelligence.document

import com.jazzbach.obsidianintelligence.shared.FileHashCalculator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class MarkdownDocumentParserTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var parser: MarkdownDocumentParser

    @BeforeEach
    fun setUp() {
        parser = MarkdownDocumentParser(
            frontmatterParser = YamlFrontmatterParser(),
            contentCleaner = ContentCleaner(),
            fileHashCalculator = FileHashCalculator()
        )
    }

    @Test
    fun `extracts title from frontmatter`() {
        val file = createFile("test.md", "---\ntitle: My Title\n---\nContent")

        val doc = parser.parse(file)

        assertThat(doc.title).isEqualTo("My Title")
    }

    @Test
    fun `extracts title from first H1 header when no frontmatter title`() {
        val file = createFile("test.md", "# Header Title\nContent")

        val doc = parser.parse(file)

        assertThat(doc.title).isEqualTo("Header Title")
    }

    @Test
    fun `uses filename as title when no frontmatter or header`() {
        val file = createFile("my-document.md", "Just content")

        val doc = parser.parse(file)

        assertThat(doc.title).isEqualTo("my-document")
    }

    @Test
    fun `collects tags from frontmatter`() {
        val file = createFile("test.md", "---\ntags: [kotlin, spring]\n---\nContent")

        val doc = parser.parse(file)

        assertThat(doc.tags).contains("kotlin", "spring")
    }

    @Test
    fun `collects inline tags from body`() {
        val file = createFile("test.md", "Content with #inline-tag and #another")

        val doc = parser.parse(file)

        assertThat(doc.tags).contains("inline-tag", "another")
    }

    @Test
    fun `merges frontmatter and inline tags without duplicates`() {
        val file = createFile("test.md", "---\ntags: [kotlin]\n---\nContent #spring #kotlin")

        val doc = parser.parse(file)

        assertThat(doc.tags).containsExactlyInAnyOrder("kotlin", "spring")
    }

    @Test
    fun `calculates word count from cleaned content`() {
        val file = createFile("test.md", "This is a simple test with seven words")

        val doc = parser.parse(file)

        assertThat(doc.wordCount).isEqualTo(8)
    }

    @Test
    fun `counts Korean characters individually`() {
        val file = createFile("test.md", "한글 테스트")

        val doc = parser.parse(file)

        // 한, 글 = 2 Korean chars + 테, 스, 트 = 3 Korean chars = 5
        assertThat(doc.wordCount).isEqualTo(5)
    }

    @Test
    fun `computes file hash`() {
        val file = createFile("test.md", "Content")

        val doc = parser.parse(file)

        assertThat(doc.fileHash).isNotBlank()
        assertThat(doc.fileHash).hasSize(32) // MD5 hex
    }

    @Test
    fun `same content produces same hash`() {
        val file1 = createFile("test1.md", "Same content")
        val file2 = createFile("test2.md", "Same content")

        val doc1 = parser.parse(file1)
        val doc2 = parser.parse(file2)

        assertThat(doc1.fileHash).isEqualTo(doc2.fileHash)
    }

    @Test
    fun `different content produces different hash`() {
        val file1 = createFile("test1.md", "Content A")
        val file2 = createFile("test2.md", "Content B")

        val doc1 = parser.parse(file1)
        val doc2 = parser.parse(file2)

        assertThat(doc1.fileHash).isNotEqualTo(doc2.fileHash)
    }

    @Test
    fun `stores cleaned content`() {
        val file = createFile("test.md", "---\ntitle: Test\n---\n# Header\n**Bold text**")

        val doc = parser.parse(file)

        assertThat(doc.cleanedContent).doesNotContain("---")
        assertThat(doc.cleanedContent).doesNotContain("#")
        assertThat(doc.cleanedContent).doesNotContain("**")
        assertThat(doc.cleanedContent).contains("Header")
        assertThat(doc.cleanedContent).contains("Bold text")
    }

    @Test
    fun `stores original content`() {
        val content = "---\ntitle: Test\n---\n# Header"
        val file = createFile("test.md", content)

        val doc = parser.parse(file)

        assertThat(doc.content).isEqualTo(content)
    }

    @Test
    fun `records file path`() {
        val file = createFile("test.md", "Content")

        val doc = parser.parse(file)

        assertThat(doc.path).isEqualTo(file)
    }

    @Test
    fun `records modified time`() {
        val file = createFile("test.md", "Content")

        val doc = parser.parse(file)

        assertThat(doc.modifiedAt).isNotNull()
    }

    private fun createFile(name: String, content: String): Path {
        val file = tempDir.resolve(name)
        Files.writeString(file, content)
        return file
    }
}
