package com.jazzbach.obsidianintelligence.tagging

import com.jazzbach.obsidianintelligence.document.Document
import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.document.Frontmatter
import com.jazzbach.obsidianintelligence.shared.MarkdownWriter
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import java.nio.file.Path
import java.time.Instant
import org.springframework.ai.document.Document as AiDocument

class SemanticTaggingServiceTest {

    private val documentParser = mockk<DocumentParser>()
    private val vectorStore = mockk<VectorStore>()
    private val tagRuleEngine = DefaultTagRuleEngine(TaggingProperties())
    private val markdownWriter = mockk<MarkdownWriter>(relaxed = true)
    private val taggingProperties = TaggingProperties()

    private lateinit var service: SemanticTaggingService

    @BeforeEach
    fun setUp() {
        service = SemanticTaggingService(
            documentParser = documentParser,
            vectorStore = vectorStore,
            tagRuleEngine = tagRuleEngine,
            markdownWriter = markdownWriter,
            taggingProperties = taggingProperties
        )
    }

    @Test
    fun `generates tags from similar documents`() {
        val path = "/vault/spring-tdd.md"
        val doc = createDocument(path, "Spring TDD Guide", "Spring Boot testing with TDD")

        val similarDoc = AiDocument.builder()
            .id("/vault/existing.md")
            .text("Existing doc about testing")
            .metadata(mapOf("tags" to "testing/tdd,frameworks/spring-boot", "filePath" to "/vault/existing.md"))
            .score(0.85)
            .build()

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(similarDoc)

        val result = service.tag(path, dryRun = true)

        assertThat(result.success).isTrue()
        assertThat(result.generatedTags).isNotEmpty()
    }

    @Test
    fun `generates pattern tags from filename`() {
        val path = "/vault/spring-boot-testing.md"
        val doc = createDocument(path, "Spring Boot Testing", "How to test Spring Boot applications")

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val result = service.tag(path, dryRun = true)

        assertThat(result.success).isTrue()
        // Should detect "spring" and "test" in filename
    }

    @Test
    fun `generates topic tags from content`() {
        val path = "/vault/architecture.md"
        val doc = createDocument(
            path, "Architecture",
            "Software architecture design with patterns and structure principles"
        )

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val result = service.tag(path, dryRun = true)

        assertThat(result.success).isTrue()
        // Should detect architecture topic from content keywords
    }

    @Test
    fun `preserves original tags in result`() {
        val path = "/vault/note.md"
        val doc = createDocument(path, "Note", "Content", tags = listOf("existing-tag"))

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val result = service.tag(path, dryRun = true)

        assertThat(result.originalTags).contains("existing-tag")
    }

    @Test
    fun `does not write file in dry run mode`() {
        val path = "/vault/note.md"
        val doc = createDocument(path, "Note", "Content about testing and development")

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        service.tag(path, dryRun = true)

        verify(exactly = 0) { markdownWriter.updateFrontmatter(any(), any()) }
    }

    @Test
    fun `writes file when not dry run and tags generated`() {
        val path = "/vault/spring-tdd.md"
        val doc = createDocument(
            path, "Spring TDD",
            "Spring Boot testing with TDD pattern architecture design"
        )

        val similarDoc = AiDocument.builder()
            .id("/vault/existing.md")
            .text("Testing doc")
            .metadata(mapOf("tags" to "testing/tdd", "filePath" to "/vault/existing.md"))
            .score(0.9)
            .build()

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(similarDoc)

        service.tag(path, dryRun = false)

        verify { markdownWriter.updateFrontmatter(Path.of(path), any()) }
    }

    @Test
    fun `handles parse failure gracefully`() {
        val path = "/vault/broken.md"
        every { documentParser.parse(Path.of(path)) } throws RuntimeException("Parse error")

        val result = service.tag(path)

        assertThat(result.success).isFalse()
        assertThat(result.errorMessage).isNotNull()
    }

    @Test
    fun `returns categorized tags`() {
        val path = "/vault/spring-tdd.md"
        val doc = createDocument(path, "Spring TDD", "Spring Boot testing with TDD")

        val similarDoc = AiDocument.builder()
            .id("/vault/existing.md")
            .text("Testing doc")
            .metadata(mapOf("tags" to "testing/tdd,frameworks/spring-boot", "filePath" to "/vault/existing.md"))
            .score(0.85)
            .build()

        every { documentParser.parse(Path.of(path)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(similarDoc)

        val result = service.tag(path, dryRun = true)

        assertThat(result.categorizedTags).isNotEmpty()
    }

    private fun createDocument(
        path: String,
        title: String,
        content: String,
        tags: List<String> = emptyList()
    ): Document = Document(
        path = Path.of(path),
        title = title,
        content = content,
        cleanedContent = content,
        frontmatter = Frontmatter(title = title, tags = tags),
        tags = tags,
        wordCount = content.split(" ").size,
        fileHash = "hash",
        modifiedAt = Instant.now()
    )
}
