package com.jazzbach.obsidianintelligence.embedding

import com.jazzbach.obsidianintelligence.document.Document
import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.document.Frontmatter
import com.jazzbach.obsidianintelligence.vault.VaultScanner
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document as AiDocument
import org.springframework.ai.vectorstore.VectorStore
import java.nio.file.Path
import java.time.Instant

class DocumentEmbeddingServiceTest {

    private val vaultScanner = mockk<VaultScanner>()
    private val documentParser = mockk<DocumentParser>()
    private val vectorStore = mockk<VectorStore>(relaxed = true)
    private val embeddingProperties = EmbeddingProperties(batchSize = 2)

    private lateinit var service: DocumentEmbeddingService

    @BeforeEach
    fun setUp() {
        service = DocumentEmbeddingService(
            vaultScanner = vaultScanner,
            documentParser = documentParser,
            vectorStore = vectorStore,
            embeddingProperties = embeddingProperties
        )
    }

    @Test
    fun `syncs vault documents to vector store`() {
        val path = Path.of("/vault/note.md")
        val doc = createDocument(path, "Test Note", "Content of test note")

        every { vaultScanner.findAllFiles() } returns listOf(path)
        every { documentParser.parse(path) } returns doc

        val result = service.syncVault()

        assertThat(result.totalFiles).isEqualTo(1)
        assertThat(result.processedFiles).isEqualTo(1)
        verify { vectorStore.add(match<List<AiDocument>> { it.size == 1 }) }
    }

    @Test
    fun `handles empty vault`() {
        every { vaultScanner.findAllFiles() } returns emptyList()

        val result = service.syncVault()

        assertThat(result.totalFiles).isEqualTo(0)
        assertThat(result.processedFiles).isEqualTo(0)
        verify(exactly = 0) { vectorStore.add(any<List<AiDocument>>()) }
    }

    @Test
    fun `skips unparseable files`() {
        val path1 = Path.of("/vault/good.md")
        val path2 = Path.of("/vault/bad.md")
        val doc = createDocument(path1, "Good", "Content")

        every { vaultScanner.findAllFiles() } returns listOf(path1, path2)
        every { documentParser.parse(path1) } returns doc
        every { documentParser.parse(path2) } throws RuntimeException("Parse error")

        val result = service.syncVault()

        assertThat(result.processedFiles).isEqualTo(1)
    }

    @Test
    fun `processes in batches`() {
        val paths = (1..5).map { Path.of("/vault/note$it.md") }
        val docs = paths.map { createDocument(it, "Note", "Content") }

        every { vaultScanner.findAllFiles() } returns paths
        paths.zip(docs).forEach { (path, doc) ->
            every { documentParser.parse(path) } returns doc
        }

        service.syncVault()

        // With batch size 2, 5 documents = 3 batches
        verify(exactly = 3) { vectorStore.add(any<List<AiDocument>>()) }
    }

    @Test
    fun `includes metadata in AI documents`() {
        val path = Path.of("/vault/note.md")
        val doc = createDocument(path, "My Note", "Content", tags = listOf("kotlin", "spring"))

        every { vaultScanner.findAllFiles() } returns listOf(path)
        every { documentParser.parse(path) } returns doc

        val captured = slot<List<AiDocument>>()
        every { vectorStore.add(capture(captured)) } just Runs

        service.syncVault()

        val aiDoc = captured.captured.first()
        assertThat(aiDoc.metadata["title"]).isEqualTo("My Note")
        assertThat(aiDoc.metadata["filePath"]).isEqualTo("/vault/note.md")
        assertThat(aiDoc.metadata["tags"]).isEqualTo("kotlin,spring")
    }

    @Test
    fun `deletes existing documents before re-indexing`() {
        val path = Path.of("/vault/note.md")
        val doc = createDocument(path, "Note", "Content")

        every { vaultScanner.findAllFiles() } returns listOf(path)
        every { documentParser.parse(path) } returns doc

        service.syncVault()

        verify { vectorStore.delete(listOf("/vault/note.md")) }
    }

    @Test
    fun `reports failed batches`() {
        val paths = (1..3).map { Path.of("/vault/note$it.md") }
        val docs = paths.map { createDocument(it, "Note", "Content") }

        every { vaultScanner.findAllFiles() } returns paths
        paths.zip(docs).forEach { (path, doc) ->
            every { documentParser.parse(path) } returns doc
        }
        // First batch succeeds, second fails
        every { vectorStore.delete(any<List<String>>()) } just Runs
        every { vectorStore.add(any<List<AiDocument>>()) } just Runs andThenAnswer { throw RuntimeException("DB error") }

        val result = service.syncVault()

        assertThat(result.failedFiles).isGreaterThan(0)
    }

    private fun createDocument(
        path: Path,
        title: String,
        content: String,
        tags: List<String> = emptyList()
    ): Document = Document(
        path = path,
        title = title,
        content = content,
        cleanedContent = content,
        frontmatter = Frontmatter(title = title, tags = tags),
        tags = tags,
        wordCount = content.split(" ").size,
        fileHash = "hash_${path.fileName}",
        modifiedAt = Instant.now()
    )
}
