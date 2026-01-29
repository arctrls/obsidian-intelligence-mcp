package com.jazzbach.obsidianintelligence.tagging

import org.springframework.stereotype.Component

@Component
class DefaultTagRuleEngine(
    private val taggingProperties: TaggingProperties
) : TagRuleEngine {

    override fun normalizeTag(tag: String): String {
        if (tag.isBlank()) return ""

        var normalized = tag.trim()

        // Lowercase
        normalized = normalized.lowercase()

        // Replace spaces with hyphens
        normalized = normalized.replace(WHITESPACE_PATTERN, "-")

        // Remove development/ prefix
        if (normalized.startsWith("development/")) {
            normalized = normalized.removePrefix("development/")
        }

        // Remove special characters (keep word chars, hyphens, slashes)
        normalized = normalized.replace(SPECIAL_CHARS_PATTERN, "")

        // Consolidate consecutive hyphens
        normalized = normalized.replace(CONSECUTIVE_HYPHENS, "-")

        // Trim leading/trailing hyphens
        normalized = normalized.trim('-')

        // Remove empty hierarchy levels (e.g., architecture//patterns)
        normalized = normalized.replace(CONSECUTIVE_SLASHES, "/")

        return normalized
    }

    override fun validateTag(tag: String): Boolean {
        if (tag.isBlank()) return false

        // Length check
        if (tag.length < MIN_LENGTH || tag.length > MAX_LENGTH) return false

        // Character pattern check
        if (!ALLOWED_CHARS.matches(tag)) return false

        // Hierarchy depth check
        if (tag.count { it == '/' } > MAX_HIERARCHY_DEPTH) return false

        // Forbidden prefix check
        if (FORBIDDEN_PREFIXES.any { tag.startsWith(it) }) return false

        // Forbidden word check
        val tagWords = tag.replace('/', '-').split('-').toSet()
        if (tagWords.any { it in FORBIDDEN_WORDS }) return false

        return true
    }

    override fun categorizeTags(tags: List<String>): Map<TagCategory, List<String>> {
        val categorized = TagCategory.entries.associateWith { mutableListOf<String>() }

        for (tag in tags) {
            val rootCategory = if ('/' in tag) tag.substringBefore('/') else tag

            // Start with mapping-based category
            var category = CATEGORY_MAPPING[rootCategory] ?: TagCategory.TOPIC

            // Override with pattern matching for specificity
            category = when {
                PATTERN_KEYWORDS.any { it in tag } -> TagCategory.PATTERNS
                FRAMEWORK_KEYWORDS.any { it in tag } -> TagCategory.FRAMEWORKS
                DOCTYPE_KEYWORDS.any { it in tag } -> TagCategory.DOCUMENT_TYPE
                SOURCE_KEYWORDS.any { it in tag } -> TagCategory.SOURCE
                else -> category
            }

            categorized[category]!!.add(tag)
        }

        return categorized
    }

    override fun applyHierarchicalStructure(concepts: List<String>): List<String> {
        return concepts.map { concept ->
            val conceptLower = concept.lowercase()

            // Direct mapping
            val directMatch = CONCEPT_HIERARCHIES[conceptLower]
            if (directMatch != null) return@map directMatch

            // Partial matching
            val partialMatch = CONCEPT_HIERARCHIES.entries.firstOrNull { (key, _) ->
                key in conceptLower
            }
            if (partialMatch != null) return@map partialMatch.value

            // Default: normalize and place under topic
            val normalized = normalizeTag(concept)
            if (validateTag(normalized)) "topic/$normalized" else normalized
        }
    }

    override fun limitTagCount(tags: List<String>, maxCount: Int): List<String> {
        val categorized = categorizeTags(tags)
        val limited = mutableListOf<String>()

        val categoryLimits = mapOf(
            TagCategory.TOPIC to taggingProperties.maxTopicTags,
            TagCategory.DOCUMENT_TYPE to taggingProperties.maxDoctypeTags,
            TagCategory.SOURCE to taggingProperties.maxSourceTags,
            TagCategory.PATTERNS to taggingProperties.maxPatternTags,
            TagCategory.FRAMEWORKS to taggingProperties.maxFrameworkTags
        )

        // Apply per-category limits, preferring more specific (deeper hierarchy) tags
        for ((category, categoryTags) in categorized) {
            val limit = categoryLimits[category] ?: 2
            if (categoryTags.size <= limit) {
                limited.addAll(categoryTags)
            } else {
                val sorted = categoryTags.sortedWith(
                    compareByDescending<String> { it.count { c -> c == '/' } }
                        .thenByDescending { it.length }
                )
                limited.addAll(sorted.take(limit))
            }
        }

        // Apply global limit with priority order
        if (limited.size > maxCount) {
            val priorityOrder = listOf(
                TagCategory.DOCUMENT_TYPE,
                TagCategory.TOPIC,
                TagCategory.FRAMEWORKS,
                TagCategory.PATTERNS,
                TagCategory.SOURCE
            )
            val finalTags = mutableListOf<String>()
            for (category in priorityOrder) {
                val categoryTags = categorized[category] ?: emptyList()
                for (tag in categoryTags) {
                    if (tag in limited && finalTags.size < maxCount) {
                        finalTags.add(tag)
                    }
                }
            }
            return finalTags
        }

        return limited
    }

    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 50
        private const val MAX_HIERARCHY_DEPTH = 4

        private val WHITESPACE_PATTERN = Regex("\\s+")
        private val SPECIAL_CHARS_PATTERN = Regex("[^\\w\\-/]")
        private val CONSECUTIVE_HYPHENS = Regex("-+")
        private val CONSECUTIVE_SLASHES = Regex("/+")
        private val ALLOWED_CHARS = Regex("^[a-z0-9\\-/]+$")

        private val FORBIDDEN_PREFIXES = listOf("resources/", "slipbox/")
        private val FORBIDDEN_WORDS = setOf("test", "temp", "tmp", "example")

        private val PATTERN_KEYWORDS = listOf("pattern", "singleton", "factory", "observer")
        private val FRAMEWORK_KEYWORDS = listOf("spring", "react", "vue", "framework")
        private val DOCTYPE_KEYWORDS = listOf("guide", "tutorial", "reference", "example")
        private val SOURCE_KEYWORDS = listOf("book", "article", "video", "conference")

        private val CATEGORY_MAPPING = mapOf(
            "architecture" to TagCategory.TOPIC,
            "design" to TagCategory.TOPIC,
            "tdd" to TagCategory.TOPIC,
            "testing" to TagCategory.TOPIC,
            "refactoring" to TagCategory.TOPIC,
            "clean-code" to TagCategory.TOPIC,
            "oop" to TagCategory.TOPIC,
            "ddd" to TagCategory.TOPIC,
            "microservices" to TagCategory.TOPIC,
            "api" to TagCategory.TOPIC,
            "database" to TagCategory.TOPIC,
            "security" to TagCategory.TOPIC,
            "performance" to TagCategory.TOPIC,
            "guide" to TagCategory.DOCUMENT_TYPE,
            "tutorial" to TagCategory.DOCUMENT_TYPE,
            "reference" to TagCategory.DOCUMENT_TYPE,
            "examples" to TagCategory.DOCUMENT_TYPE,
            "notes" to TagCategory.DOCUMENT_TYPE,
            "summary" to TagCategory.DOCUMENT_TYPE,
            "book" to TagCategory.SOURCE,
            "article" to TagCategory.SOURCE,
            "video" to TagCategory.SOURCE,
            "conference" to TagCategory.SOURCE,
            "blog" to TagCategory.SOURCE,
            "documentation" to TagCategory.SOURCE,
            "patterns" to TagCategory.PATTERNS,
            "singleton" to TagCategory.PATTERNS,
            "factory" to TagCategory.PATTERNS,
            "observer" to TagCategory.PATTERNS,
            "strategy" to TagCategory.PATTERNS,
            "template" to TagCategory.PATTERNS,
            "frameworks" to TagCategory.FRAMEWORKS,
            "spring" to TagCategory.FRAMEWORKS,
            "spring-boot" to TagCategory.FRAMEWORKS,
            "react" to TagCategory.FRAMEWORKS,
            "vue" to TagCategory.FRAMEWORKS,
            "angular" to TagCategory.FRAMEWORKS,
            "django" to TagCategory.FRAMEWORKS,
            "fastapi" to TagCategory.FRAMEWORKS
        )

        private val CONCEPT_HIERARCHIES = mapOf(
            "spring" to "frameworks/spring-boot",
            "test" to "testing/unit",
            "tdd" to "testing/tdd",
            "architecture" to "architecture/design",
            "pattern" to "patterns/design-patterns",
            "refactor" to "practices/refactoring",
            "clean" to "practices/clean-code",
            "database" to "data/database",
            "api" to "architecture/api",
            "microservice" to "architecture/microservices",
            "security" to "security/general",
            "performance" to "performance/optimization"
        )
    }
}
