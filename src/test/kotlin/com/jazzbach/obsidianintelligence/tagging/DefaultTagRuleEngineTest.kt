package com.jazzbach.obsidianintelligence.tagging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DefaultTagRuleEngineTest {

    private lateinit var engine: DefaultTagRuleEngine

    @BeforeEach
    fun setUp() {
        engine = DefaultTagRuleEngine(
            TaggingProperties(
                maxTopicTags = 4,
                maxDoctypeTags = 1,
                maxSourceTags = 1,
                maxPatternTags = 3,
                maxFrameworkTags = 2
            )
        )
    }

    @Nested
    inner class NormalizeTag {

        @Test
        fun `converts to lowercase`() {
            assertThat(engine.normalizeTag("Architecture")).isEqualTo("architecture")
        }

        @Test
        fun `replaces spaces with hyphens`() {
            assertThat(engine.normalizeTag("Clean Code")).isEqualTo("clean-code")
        }

        @Test
        fun `removes development prefix`() {
            assertThat(engine.normalizeTag("Development/Spring Boot"))
                .isEqualTo("spring-boot")
        }

        @Test
        fun `removes special characters`() {
            assertThat(engine.normalizeTag("spring@boot!")).isEqualTo("springboot")
        }

        @Test
        fun `consolidates consecutive hyphens`() {
            assertThat(engine.normalizeTag("clean--code")).isEqualTo("clean-code")
        }

        @Test
        fun `trims hyphens from edges`() {
            assertThat(engine.normalizeTag("-kotlin-")).isEqualTo("kotlin")
        }

        @Test
        fun `removes empty hierarchy levels`() {
            assertThat(engine.normalizeTag("architecture//patterns"))
                .isEqualTo("architecture/patterns")
        }

        @Test
        fun `handles blank input`() {
            assertThat(engine.normalizeTag("")).isEmpty()
            assertThat(engine.normalizeTag("   ")).isEmpty()
        }

        @Test
        fun `preserves hierarchical slashes`() {
            assertThat(engine.normalizeTag("architecture/microservices"))
                .isEqualTo("architecture/microservices")
        }
    }

    @Nested
    inner class ValidateTag {

        @Test
        fun `accepts valid simple tag`() {
            assertThat(engine.validateTag("kotlin")).isTrue()
        }

        @Test
        fun `accepts valid hierarchical tag`() {
            assertThat(engine.validateTag("architecture/microservices")).isTrue()
        }

        @Test
        fun `accepts tag with hyphens`() {
            assertThat(engine.validateTag("clean-code")).isTrue()
        }

        @Test
        fun `rejects blank tag`() {
            assertThat(engine.validateTag("")).isFalse()
        }

        @Test
        fun `rejects too short tag`() {
            assertThat(engine.validateTag("a")).isFalse()
        }

        @Test
        fun `rejects too long tag`() {
            val longTag = "a".repeat(51)
            assertThat(engine.validateTag(longTag)).isFalse()
        }

        @Test
        fun `rejects uppercase characters`() {
            assertThat(engine.validateTag("Kotlin")).isFalse()
        }

        @Test
        fun `rejects special characters`() {
            assertThat(engine.validateTag("spring@boot")).isFalse()
        }

        @Test
        fun `rejects too deep hierarchy`() {
            assertThat(engine.validateTag("a/b/c/d/e/f")).isFalse()
        }

        @Test
        fun `rejects forbidden prefix resources`() {
            assertThat(engine.validateTag("resources/books")).isFalse()
        }

        @Test
        fun `rejects forbidden prefix slipbox`() {
            assertThat(engine.validateTag("slipbox/notes")).isFalse()
        }

        @Test
        fun `rejects tag containing forbidden word`() {
            assertThat(engine.validateTag("test")).isFalse()
            assertThat(engine.validateTag("temp")).isFalse()
        }
    }

    @Nested
    inner class CategorizeTags {

        @Test
        fun `categorizes topic tags`() {
            val result = engine.categorizeTags(listOf("architecture/microservices", "tdd"))
            assertThat(result[TagCategory.TOPIC]).contains("tdd")
        }

        @Test
        fun `categorizes framework tags`() {
            val result = engine.categorizeTags(listOf("spring-boot", "frameworks/react"))
            assertThat(result[TagCategory.FRAMEWORKS]).isNotEmpty()
        }

        @Test
        fun `categorizes pattern tags`() {
            val result = engine.categorizeTags(listOf("patterns/singleton", "factory"))
            assertThat(result[TagCategory.PATTERNS]).isNotEmpty()
        }

        @Test
        fun `categorizes document type tags`() {
            val result = engine.categorizeTags(listOf("guide/tutorial"))
            assertThat(result[TagCategory.DOCUMENT_TYPE]).contains("guide/tutorial")
        }

        @Test
        fun `categorizes source tags`() {
            val result = engine.categorizeTags(listOf("book/clean-code"))
            assertThat(result[TagCategory.SOURCE]).contains("book/clean-code")
        }

        @Test
        fun `defaults to topic for unknown tags`() {
            val result = engine.categorizeTags(listOf("unknown-concept"))
            assertThat(result[TagCategory.TOPIC]).contains("unknown-concept")
        }
    }

    @Nested
    inner class ApplyHierarchicalStructure {

        @Test
        fun `maps spring to frameworks hierarchy`() {
            val result = engine.applyHierarchicalStructure(listOf("spring"))
            assertThat(result).contains("frameworks/spring-boot")
        }

        @Test
        fun `maps tdd to testing hierarchy`() {
            val result = engine.applyHierarchicalStructure(listOf("tdd"))
            assertThat(result).contains("testing/tdd")
        }

        @Test
        fun `maps architecture concept`() {
            val result = engine.applyHierarchicalStructure(listOf("architecture"))
            assertThat(result).contains("architecture/design")
        }

        @Test
        fun `uses partial matching`() {
            val result = engine.applyHierarchicalStructure(listOf("microservice-based"))
            assertThat(result).contains("architecture/microservices")
        }

        @Test
        fun `places unmatched concepts under topic`() {
            val result = engine.applyHierarchicalStructure(listOf("kotlin"))
            assertThat(result).contains("topic/kotlin")
        }
    }

    @Nested
    inner class LimitTagCount {

        @Test
        fun `returns all tags when under limit`() {
            val tags = listOf("kotlin", "spring-boot")
            val result = engine.limitTagCount(tags, 10)
            assertThat(result).hasSize(2)
        }

        @Test
        fun `limits tags when over limit`() {
            val tags = (1..20).map { "topic-$it" }
            val result = engine.limitTagCount(tags, 10)
            assertThat(result.size).isLessThanOrEqualTo(10)
        }

        @Test
        fun `respects category limits`() {
            val tags = listOf(
                "architecture/design", "architecture/microservices",
                "architecture/hexagonal", "architecture/clean",
                "architecture/layered", "architecture/event-driven"
            )
            val result = engine.limitTagCount(tags, 10)
            // Should be limited by maxTopicTags = 4
            assertThat(result.size).isLessThanOrEqualTo(4)
        }

        @Test
        fun `prefers more specific tags (deeper hierarchy)`() {
            // All tags categorized as Topic (none contain pattern/framework/etc keywords)
            val tags = listOf(
                "architecture", "architecture/microservices/event-driven",
                "architecture/microservices", "architecture/clean",
                "architecture/hexagonal"
            )
            val result = engine.limitTagCount(tags, 10)
            // maxTopicTags=4, should prefer deeper hierarchy (more slashes first)
            assertThat(result).hasSize(4)
            assertThat(result).contains("architecture/microservices/event-driven")
        }
    }
}
