package com.jazzbach.obsidianintelligence.expansion

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QueryExpansionServiceTest {

    private lateinit var service: QueryExpansionService

    @BeforeEach
    fun setUp() {
        service = QueryExpansionService()
    }

    @Test
    fun `expands Korean query with synonyms`() {
        val result = service.expand("코틀린 테스트")

        assertThat(result.originalQuery).isEqualTo("코틀린 테스트")
        assertThat(result.expandedTerms).isNotEmpty()
        assertThat(result.expandedTerms).anyMatch { it.contains("kotlin", ignoreCase = true) }
        assertThat(result.expandedTerms).anyMatch { it.contains("test", ignoreCase = true) }
    }

    @Test
    fun `expands English query with synonyms`() {
        val result = service.expand("kotlin testing")

        assertThat(result.originalQuery).isEqualTo("kotlin testing")
        assertThat(result.expandedTerms).isNotEmpty()
        assertThat(result.expandedTerms).anyMatch { it.contains("코틀린") }
    }

    @Test
    fun `returns empty expanded terms for unknown query`() {
        val result = service.expand("xyzabc123")

        assertThat(result.originalQuery).isEqualTo("xyzabc123")
        assertThat(result.expandedTerms).isEmpty()
    }

    @Test
    fun `generates HyDE document for Korean query`() {
        val result = service.expand("스프링 보안")

        assertThat(result.hydeDocument).contains("스프링 보안")
        assertThat(result.hydeDocument).contains("문서")
    }

    @Test
    fun `generates HyDE document for English query`() {
        val result = service.expand("spring security")

        assertThat(result.hydeDocument).contains("spring security")
        assertThat(result.hydeDocument).contains("document")
    }

    @Test
    fun `combined query includes original and expanded terms`() {
        val result = service.expand("코틀린")

        val combined = result.combinedQuery()
        assertThat(combined).contains("코틀린")
        assertThat(result.allTerms).contains("코틀린")
    }
}
