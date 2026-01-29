package com.jazzbach.obsidianintelligence.document

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YamlFrontmatterParserTest {

    private val parser = YamlFrontmatterParser()

    @Test
    fun `parses title from frontmatter`() {
        val content = "---\ntitle: My Document\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.title).isEqualTo("My Document")
    }

    @Test
    fun `parses tags as list`() {
        val content = "---\ntags:\n  - kotlin\n  - spring\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.tags).containsExactly("kotlin", "spring")
    }

    @Test
    fun `parses tags as inline list`() {
        val content = "---\ntags: [kotlin, spring]\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.tags).containsExactly("kotlin", "spring")
    }

    @Test
    fun `parses tags as comma-separated string`() {
        val content = "---\ntags: kotlin, spring\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.tags).containsExactly("kotlin", "spring")
    }

    @Test
    fun `parses aliases`() {
        val content = "---\naliases:\n  - alias1\n  - alias2\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.aliases).containsExactly("alias1", "alias2")
    }

    @Test
    fun `preserves all properties`() {
        val content = "---\ntitle: Test\ncustom: value\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.properties).containsEntry("custom", "value")
    }

    @Test
    fun `returns empty frontmatter when no frontmatter present`() {
        val content = "Just body content"

        val result = parser.parse(content)

        assertThat(result.title).isNull()
        assertThat(result.tags).isEmpty()
    }

    @Test
    fun `returns empty frontmatter for malformed YAML`() {
        val content = "---\n: invalid: yaml: [[\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.title).isNull()
    }

    @Test
    fun `hasFrontmatter returns true when present`() {
        assertThat(parser.hasFrontmatter("---\ntitle: Test\n---\nBody")).isTrue()
    }

    @Test
    fun `hasFrontmatter returns false when absent`() {
        assertThat(parser.hasFrontmatter("Just content")).isFalse()
    }

    @Test
    fun `extractBody returns content after frontmatter`() {
        val content = "---\ntitle: Test\n---\nBody content"

        val result = parser.extractBody(content)

        assertThat(result).isEqualTo("Body content")
    }

    @Test
    fun `extractBody returns entire content when no frontmatter`() {
        val content = "Body content only"

        val result = parser.extractBody(content)

        assertThat(result).isEqualTo("Body content only")
    }

    @Test
    fun `handles empty tags list`() {
        val content = "---\ntitle: Test\ntags: []\n---\nBody"

        val result = parser.parse(content)

        assertThat(result.tags).isEmpty()
    }

    @Test
    fun `handles frontmatter with no closing delimiter`() {
        val content = "---\ntitle: Test\nBody without closing"

        val result = parser.parse(content)

        assertThat(result.title).isNull()
    }
}
