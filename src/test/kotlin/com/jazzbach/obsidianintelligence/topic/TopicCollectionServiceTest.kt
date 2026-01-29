package com.jazzbach.obsidianintelligence.topic

import com.jazzbach.obsidianintelligence.search.SearchDocuments
import com.jazzbach.obsidianintelligence.search.SearchQuery
import com.jazzbach.obsidianintelligence.search.SearchResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TopicCollectionServiceTest {

    private val searchDocuments = mockk<SearchDocuments>()
    private val topicProperties = TopicProperties(defaultTopK = 50, similarityThreshold = 0.3, minGroupSize = 2)
    private lateinit var service: TopicCollectionService

    @BeforeEach
    fun setUp() {
        service = TopicCollectionService(searchDocuments, topicProperties)
    }

    @Test
    fun `returns empty collection when no documents found`() {
        every { searchDocuments.search(any<SearchQuery>()) } returns emptyList()

        val result = service.collect(TopicRequest(topic = "nonexistent"))

        assertThat(result.topic).isEqualTo("nonexistent")
        assertThat(result.totalDocuments).isEqualTo(0)
        assertThat(result.groups).hasSize(0)
    }

    @Test
    fun `collects documents matching topic`() {
        val searchResults = listOf(
            createSearchResult("/vault/kotlin.md", "Kotlin Guide", listOf("kotlin", "programming")),
            createSearchResult("/vault/spring.md", "Spring Boot", listOf("spring", "kotlin"))
        )
        every { searchDocuments.search(any<SearchQuery>()) } returns searchResults

        val result = service.collect(TopicRequest(topic = "kotlin"))

        assertThat(result.totalDocuments).isEqualTo(2)
    }

    @Test
    fun `groups documents by most frequent tags`() {
        val searchResults = listOf(
            createSearchResult("/vault/a.md", "A", listOf("kotlin", "spring")),
            createSearchResult("/vault/b.md", "B", listOf("kotlin", "testing")),
            createSearchResult("/vault/c.md", "C", listOf("kotlin", "spring")),
            createSearchResult("/vault/d.md", "D", listOf("java"))
        )
        every { searchDocuments.search(any<SearchQuery>()) } returns searchResults

        val result = service.collect(TopicRequest(topic = "programming"))

        val kotlinGroup = result.groups.find { it.tagName == "kotlin" }
        assertThat(kotlinGroup).isNotNull
        assertThat(kotlinGroup!!.documents.size).isGreaterThanOrEqualTo(2)
    }

    @Test
    fun `puts ungrouped documents into 기타 group`() {
        val searchResults = listOf(
            createSearchResult("/vault/a.md", "A", listOf("unique-tag-1")),
            createSearchResult("/vault/b.md", "B", listOf("unique-tag-2"))
        )
        every { searchDocuments.search(any<SearchQuery>()) } returns searchResults

        val result = service.collect(TopicRequest(topic = "misc"))

        val etcGroup = result.groups.find { it.tagName == "기타" }
        assertThat(etcGroup).isNotNull
        assertThat(etcGroup!!.documents).hasSize(2)
    }

    @Test
    fun `filters documents by minimum word count`() {
        val searchResults = listOf(
            createSearchResult("/vault/long.md", "Long", emptyList(), wordCount = 500),
            createSearchResult("/vault/short.md", "Short", emptyList(), wordCount = 10)
        )
        every { searchDocuments.search(any<SearchQuery>()) } returns searchResults

        val result = service.collect(TopicRequest(topic = "test", minWordCount = 100))

        assertThat(result.totalDocuments).isEqualTo(1)
    }

    @Test
    fun `calculates statistics correctly`() {
        val searchResults = listOf(
            createSearchResult("/vault/a.md", "A", listOf("kotlin"), wordCount = 100),
            createSearchResult("/vault/b.md", "B", listOf("kotlin"), wordCount = 200),
            createSearchResult("/vault/c.md", "C", listOf("kotlin"), wordCount = 300)
        )
        every { searchDocuments.search(any<SearchQuery>()) } returns searchResults

        val result = service.collect(TopicRequest(topic = "kotlin"))

        assertThat(result.statistics.minWordCount).isEqualTo(100)
        assertThat(result.statistics.maxWordCount).isEqualTo(300)
        assertThat(result.statistics.avgWordCount).isEqualTo(200.0)
    }

    @Test
    fun `finds related topics from tag frequency`() {
        val searchResults = listOf(
            createSearchResult("/vault/a.md", "A", listOf("kotlin", "spring")),
            createSearchResult("/vault/b.md", "B", listOf("kotlin", "spring")),
            createSearchResult("/vault/c.md", "C", listOf("kotlin", "testing"))
        )
        every { searchDocuments.search(any<SearchQuery>()) } returns searchResults

        val result = service.collect(TopicRequest(topic = "development"))

        assertThat(result.relatedTopics).contains("kotlin")
        assertThat(result.relatedTopics).contains("spring")
    }

    @Test
    fun `uses default properties when request values are zero or unset`() {
        every { searchDocuments.search(any<SearchQuery>()) } returns emptyList()

        val result = service.collect(TopicRequest(topic = "test", topK = 0, similarityThreshold = 0.0))

        assertThat(result.topic).isEqualTo("test")
        assertThat(result.totalDocuments).isEqualTo(0)
    }

    private fun createSearchResult(
        filePath: String,
        title: String,
        tags: List<String>,
        wordCount: Int = 100,
        score: Double = 0.8
    ): SearchResult {
        return SearchResult(
            filePath = filePath,
            title = title,
            score = score,
            snippet = "Snippet for $title",
            tags = tags,
            wordCount = wordCount
        )
    }
}
