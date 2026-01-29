package com.jazzbach.obsidianintelligence.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KeywordSearchServiceTest {

    private lateinit var service: KeywordSearchService

    @BeforeEach
    fun setUp() {
        service = KeywordSearchService()
    }

    @Test
    fun `returns zero score for empty keywords`() {
        val score = service.calculateScore(
            title = "Kotlin Guide",
            tags = listOf("kotlin"),
            content = "Some content about kotlin",
            keywords = emptyList()
        )

        assertThat(score).isEqualTo(0.0)
    }

    @Test
    fun `adds title weight when keyword matches title`() {
        val score = service.calculateScore(
            title = "Kotlin Guide",
            tags = emptyList(),
            content = "",
            keywords = listOf("kotlin")
        )

        assertThat(score).isGreaterThan(0.0)
        assertThat(score).isGreaterThanOrEqualTo(KeywordSearchService.TITLE_WEIGHT)
    }

    @Test
    fun `adds tag weight when keyword matches tag`() {
        val score = service.calculateScore(
            title = "Unrelated Title",
            tags = listOf("kotlin", "spring"),
            content = "",
            keywords = listOf("kotlin")
        )

        assertThat(score).isGreaterThanOrEqualTo(KeywordSearchService.TAG_WEIGHT)
    }

    @Test
    fun `adds content weight based on frequency`() {
        val score = service.calculateScore(
            title = "Unrelated",
            tags = emptyList(),
            content = "kotlin is great. kotlin works well. kotlin is fast.",
            keywords = listOf("kotlin")
        )

        assertThat(score).isGreaterThan(0.0)
    }

    @Test
    fun `applies coverage ratio for partial keyword matching`() {
        val fullMatchScore = service.calculateScore(
            title = "Kotlin Spring",
            tags = emptyList(),
            content = "kotlin and spring content",
            keywords = listOf("kotlin", "spring")
        )

        val partialMatchScore = service.calculateScore(
            title = "Kotlin Guide",
            tags = emptyList(),
            content = "kotlin content only",
            keywords = listOf("kotlin", "nonexistent")
        )

        assertThat(fullMatchScore).isGreaterThan(partialMatchScore)
    }
}
