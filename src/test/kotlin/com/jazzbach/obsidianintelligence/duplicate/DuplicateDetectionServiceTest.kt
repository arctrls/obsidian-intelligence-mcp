package com.jazzbach.obsidianintelligence.duplicate

import com.jazzbach.obsidianintelligence.document.Document
import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.document.Frontmatter
import com.jazzbach.obsidianintelligence.vault.VaultScanner
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import java.nio.file.Path
import java.time.Instant

class DuplicateDetectionServiceTest {

    private val vaultScanner = mockk<VaultScanner>()
    private val documentParser = mockk<DocumentParser>()
    private val vectorStore = mockk<VectorStore>()
    private val properties = DuplicateProperties(similarityThreshold = 0.85, minWordCount = 50)
    private lateinit var service: DuplicateDetectionService

    @BeforeEach
    fun setUp() {
        service = DuplicateDetectionService(vaultScanner, documentParser, vectorStore, properties)
    }

    @Test
    fun `returns empty analysis when no files found`() {
        every { vaultScanner.findAllFiles() } returns emptyList()

        val result = service.detect()

        assertThat(result.totalDocumentsScanned).isEqualTo(0)
        assertThat(result.duplicateGroupCount).isEqualTo(0)
        assertThat(result.groups).isEmpty()
    }

    @Test
    fun `filters out documents below minimum word count`() {
        val path = Path.of("/vault/short.md")
        every { vaultScanner.findAllFiles() } returns listOf(path)
        every { documentParser.parse(path) } returns createDocument(path, wordCount = 10)

        val result = service.detect()

        assertThat(result.totalDocumentsScanned).isEqualTo(0)
    }

    @Test
    fun `detects duplicate group when similarity exceeds threshold`() {
        val path1 = Path.of("/vault/original.md")
        val path2 = Path.of("/vault/copy.md")

        every { vaultScanner.findAllFiles() } returns listOf(path1, path2)
        every { documentParser.parse(path1) } returns createDocument(path1, wordCount = 200, title = "Original")
        every { documentParser.parse(path2) } returns createDocument(path2, wordCount = 150, title = "Copy")

        val aiDoc = createAiDocument("/vault/copy.md", "Copy", 150, 0.92)
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(aiDoc)

        val result = service.detect()

        assertThat(result.duplicateGroupCount).isEqualTo(1)
        assertThat(result.groups[0].master.filePath).isEqualTo("/vault/original.md")
        assertThat(result.groups[0].duplicates).hasSize(1)
        assertThat(result.groups[0].duplicates[0].filePath).isEqualTo("/vault/copy.md")
    }

    @Test
    fun `selects document with highest word count as master`() {
        val path1 = Path.of("/vault/short-original.md")
        val path2 = Path.of("/vault/longer-version.md")

        every { vaultScanner.findAllFiles() } returns listOf(path1, path2)
        every { documentParser.parse(path1) } returns createDocument(path1, wordCount = 100, title = "Short")
        every { documentParser.parse(path2) } returns createDocument(path2, wordCount = 300, title = "Longer")

        val aiDoc = createAiDocument("/vault/longer-version.md", "Longer", 300, 0.90)
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(aiDoc)

        val result = service.detect()

        assertThat(result.groups[0].master.title).isEqualTo("Longer")
        assertThat(result.groups[0].master.wordCount).isEqualTo(300)
    }

    @Test
    fun `does not create group when no similar documents found`() {
        val path1 = Path.of("/vault/unique.md")

        every { vaultScanner.findAllFiles() } returns listOf(path1)
        every { documentParser.parse(path1) } returns createDocument(path1, wordCount = 100, title = "Unique")
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val result = service.detect()

        assertThat(result.duplicateGroupCount).isEqualTo(0)
        assertThat(result.groups).isEmpty()
    }

    @Test
    fun `visited documents are not included in subsequent groups`() {
        val path1 = Path.of("/vault/a.md")
        val path2 = Path.of("/vault/b.md")
        val path3 = Path.of("/vault/c.md")

        every { vaultScanner.findAllFiles() } returns listOf(path1, path2, path3)
        every { documentParser.parse(path1) } returns createDocument(path1, wordCount = 100, title = "A")
        every { documentParser.parse(path2) } returns createDocument(path2, wordCount = 100, title = "B")
        every { documentParser.parse(path3) } returns createDocument(path3, wordCount = 100, title = "C")

        val aiDocB = createAiDocument("/vault/b.md", "B", 100, 0.90)
        val aiDocC = createAiDocument("/vault/c.md", "C", 100, 0.88)

        // First scan of A finds B as duplicate
        every { vectorStore.similaritySearch(match<SearchRequest> {
            true
        }) } returnsMany listOf(
            listOf(aiDocB),   // A's search finds B
            emptyList(),      // C's search finds nothing (B already visited)
        )

        val result = service.detect()

        assertThat(result.duplicateGroupCount).isEqualTo(1)
    }

    @Test
    fun `handles parse errors gracefully`() {
        val path1 = Path.of("/vault/good.md")
        val path2 = Path.of("/vault/bad.md")

        every { vaultScanner.findAllFiles() } returns listOf(path1, path2)
        every { documentParser.parse(path1) } returns createDocument(path1, wordCount = 100)
        every { documentParser.parse(path2) } throws RuntimeException("Parse error")
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val result = service.detect()

        assertThat(result.totalDocumentsScanned).isEqualTo(1)
    }

    private fun createDocument(
        path: Path,
        wordCount: Int = 100,
        title: String = "Test Document"
    ): Document {
        val content = "a ".repeat(wordCount)
        return Document(
            path = path,
            title = title,
            content = content,
            cleanedContent = content,
            frontmatter = Frontmatter(),
            tags = emptyList(),
            wordCount = wordCount,
            fileHash = "hash-${path.fileName}",
            modifiedAt = Instant.now()
        )
    }

    private fun createAiDocument(
        filePath: String,
        title: String,
        wordCount: Int,
        score: Double
    ): org.springframework.ai.document.Document {
        val metadata = mutableMapOf<String, Any>(
            "filePath" to filePath,
            "title" to title,
            "wordCount" to wordCount,
            "tags" to ""
        )
        return org.springframework.ai.document.Document.builder()
            .id(filePath)
            .text("content")
            .metadata(metadata)
            .score(score)
            .build()
    }
}
