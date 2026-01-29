package com.jazzbach.obsidianintelligence.search

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore

class SemanticSearchServiceTest {

    private val vectorStore = mockk<VectorStore>()
    private val searchProperties = SearchProperties(defaultTopK = 10, similarityThreshold = 0.3)
    private lateinit var service: SemanticSearchService

    @BeforeEach
    fun setUp() {
        service = SemanticSearchService(vectorStore, searchProperties)
    }

    @Test
    fun `returns search results from vector store`() {
        val aiDoc = createAiDocument(
            content = "Kotlin is a modern programming language",
            filePath = "/vault/kotlin-intro.md",
            title = "Kotlin Introduction",
            tags = "kotlin,programming",
            wordCount = 10,
            score = 0.85
        )

        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(aiDoc)

        val results = service.search(SearchQuery(text = "kotlin programming"))

        assertThat(results).hasSize(1)
        assertThat(results[0].filePath).isEqualTo("/vault/kotlin-intro.md")
        assertThat(results[0].title).isEqualTo("Kotlin Introduction")
        assertThat(results[0].score).isEqualTo(0.85)
        assertThat(results[0].tags).containsExactly("kotlin", "programming")
    }

    @Test
    fun `returns empty list when no results`() {
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val results = service.search(SearchQuery(text = "nonexistent"))

        assertThat(results).isEmpty()
    }

    @Test
    fun `filters results by exclude paths`() {
        val doc1 = createAiDocument(filePath = "/vault/notes/good.md", title = "Good")
        val doc2 = createAiDocument(filePath = "/vault/archive/old.md", title = "Old")

        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(doc1, doc2)

        val results = service.search(
            SearchQuery(text = "test", excludePaths = listOf("archive"))
        )

        assertThat(results).hasSize(1)
        assertThat(results[0].filePath).isEqualTo("/vault/notes/good.md")
    }

    @Test
    fun `filters results by tags`() {
        val doc1 = createAiDocument(filePath = "/vault/a.md", title = "A", tags = "kotlin,spring")
        val doc2 = createAiDocument(filePath = "/vault/b.md", title = "B", tags = "java,maven")

        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(doc1, doc2)

        val results = service.search(
            SearchQuery(text = "test", tags = listOf("kotlin"))
        )

        assertThat(results).hasSize(1)
        assertThat(results[0].title).isEqualTo("A")
    }

    @Test
    fun `returns all results when no tag filter specified`() {
        val doc1 = createAiDocument(filePath = "/vault/a.md", title = "A", tags = "kotlin")
        val doc2 = createAiDocument(filePath = "/vault/b.md", title = "B", tags = "java")

        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(doc1, doc2)

        val results = service.search(SearchQuery(text = "test"))

        assertThat(results).hasSize(2)
    }

    @Test
    fun `generates snippet from content`() {
        val aiDoc = createAiDocument(
            content = "This is about Kotlin. Kotlin is great for development. It was created by JetBrains.",
            filePath = "/vault/kotlin.md",
            title = "Kotlin"
        )

        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns listOf(aiDoc)

        val results = service.search(SearchQuery(text = "Kotlin development"))

        assertThat(results[0].snippet).isNotBlank()
    }

    @Test
    fun `uses default properties when query values are zero`() {
        every { vectorStore.similaritySearch(any<SearchRequest>()) } returns emptyList()

        val results = service.search(
            SearchQuery(text = "test", topK = 0, similarityThreshold = 0.0)
        )

        assertThat(results).isEmpty()
    }

    private fun createAiDocument(
        content: String = "Test content",
        filePath: String = "/vault/test.md",
        title: String = "Test",
        tags: String = "",
        wordCount: Int = 5,
        score: Double = 0.8
    ): Document {
        val metadata = mutableMapOf<String, Any>(
            "filePath" to filePath,
            "title" to title,
            "tags" to tags,
            "wordCount" to wordCount
        )
        return Document.builder()
            .id(filePath)
            .text(content)
            .metadata(metadata)
            .score(score)
            .build()
    }
}
