package com.jazzbach.obsidianintelligence.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MarkdownWriterTest {

    private val writer = MarkdownWriter()

    @Test
    fun `updates existing frontmatter property`() {
        val content = "---\ntitle: Old\n---\nBody"

        val result = writer.updateFrontmatterContent(content, mapOf("title" to "New"))

        assertThat(result).contains("title: New")
        assertThat(result).contains("Body")
    }

    @Test
    fun `adds new property to existing frontmatter`() {
        val content = "---\ntitle: Test\n---\nBody"

        val result = writer.updateFrontmatterContent(content, mapOf("tags" to listOf("kotlin", "spring")))

        assertThat(result).contains("title: Test")
        assertThat(result).contains("tags:")
        assertThat(result).contains("Body")
    }

    @Test
    fun `creates frontmatter when none exists`() {
        val content = "Body only"

        val result = writer.updateFrontmatterContent(content, mapOf("title" to "New Title"))

        assertThat(result).startsWith("---\n")
        assertThat(result).contains("title: New Title")
        assertThat(result).contains("Body only")
    }

    @Test
    fun `updates section replaces existing section`() {
        val content = "# Title\n\n## 관련 문서\nOld content\n\n## Other Section"

        val result = writer.updateSection(content, "## 관련 문서", "New content\n")

        assertThat(result).contains("## 관련 문서\nNew content")
        assertThat(result).contains("## Other Section")
        assertThat(result).doesNotContain("Old content")
    }

    @Test
    fun `updates section appends when section does not exist`() {
        val content = "# Title\n\nSome content"

        val result = writer.updateSection(content, "## 관련 문서", "- [[Related Note]]")

        assertThat(result).contains("# Title")
        assertThat(result).contains("Some content")
        assertThat(result).contains("## 관련 문서\n- [[Related Note]]")
    }
}
