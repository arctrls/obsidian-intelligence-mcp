package com.jazzbach.obsidianintelligence.related

import com.jazzbach.obsidianintelligence.document.Document
import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.document.Frontmatter
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import java.nio.file.Path
import java.time.Instant
import org.springframework.ai.document.Document as AiDocument

class RelatedDocsServiceTest {

    private val documentParser = mockk<DocumentParser>()
    private val vectorStore = mockk<VectorStore>()
    private val properties = RelatedDocsProperties(defaultTopK = 5, similarityThreshold = 0.3)
    private lateinit var service: RelatedDocsService

    @BeforeEach
    fun setUp() {
        service = RelatedDocsService(documentParser, vectorStore, properties)
    }

    @Test
    fun `finds related documents excluding self`() {
        val filePath = "/vault/note1.md"
        val doc = createDocument(filePath)

        val selfDoc = createAiDocument("/vault/note1.md", "Self", 0.99)
        val relatedDoc = createAiDocument("/vault/note2.md", "Related Note", 0.85)

        every { documentParser.parse(Path.of(filePath)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(selfDoc, relatedDoc)

        val results = service.findRelated(filePath)

        assertThat(results).hasSize(1)
        assertThat(results[0].filePath).isEqualTo("/vault/note2.md")
        assertThat(results[0].title).isEqualTo("Related Note")
        assertThat(results[0].score).isEqualTo(0.85)
    }

    @Test
    fun `returns empty list when no related docs found`() {
        val filePath = "/vault/isolated.md"
        val doc = createDocument(filePath)

        every { documentParser.parse(Path.of(filePath)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val results = service.findRelated(filePath)

        assertThat(results).isEmpty()
    }

    @Test
    fun `limits results to topK`() {
        val filePath = "/vault/note.md"
        val doc = createDocument(filePath)

        val aiDocs = (1..10).map { createAiDocument("/vault/related$it.md", "Related $it", 0.9 - it * 0.05) }

        every { documentParser.parse(Path.of(filePath)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns aiDocs

        val results = service.findRelated(filePath, topK = 3)

        assertThat(results).hasSize(3)
    }

    @Test
    fun `extracts tags from metadata`() {
        val filePath = "/vault/note.md"
        val doc = createDocument(filePath)

        val aiDoc = createAiDocument("/vault/tagged.md", "Tagged", 0.8, "kotlin,spring-boot")

        every { documentParser.parse(Path.of(filePath)) } returns doc
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(aiDoc)

        val results = service.findRelated(filePath)

        assertThat(results[0].tags).containsExactly("kotlin", "spring-boot")
    }

    @Test
    fun `handles parse failure gracefully`() {
        every { documentParser.parse(any()) } throws RuntimeException("Parse error")

        val results = service.findRelated("/vault/broken.md")

        assertThat(results).isEmpty()
    }

    private fun createDocument(filePath: String): Document = Document(
        path = Path.of(filePath),
        title = "Test Document",
        content = "Some test content for similarity search",
        cleanedContent = "Some test content for similarity search",
        frontmatter = Frontmatter(),
        tags = emptyList(),
        wordCount = 6,
        fileHash = "hash",
        modifiedAt = Instant.now()
    )

    private fun createAiDocument(
        filePath: String,
        title: String,
        score: Double,
        tags: String = ""
    ): AiDocument {
        return AiDocument.builder()
            .id(filePath)
            .text("Document content")
            .metadata(mapOf(
                "filePath" to filePath,
                "title" to title,
                "tags" to tags
            ))
            .score(score)
            .build()
    }
}
