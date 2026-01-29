package com.jazzbach.obsidianintelligence.document

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContentCleanerTest {

    private val cleaner = ContentCleaner()

    @Test
    fun `removes YAML frontmatter`() {
        val content = """
            |---
            |title: Test
            |tags: [a, b]
            |---
            |Hello world
        """.trimMargin()

        val result = cleaner.clean(content)

        assertThat(result).isEqualTo("Hello world")
    }

    @Test
    fun `removes fenced code blocks`() {
        val content = """
            |Some text
            |```kotlin
            |fun main() = println("Hello")
            |```
            |More text
        """.trimMargin()

        val result = cleaner.clean(content)

        assertThat(result).contains("Some text")
        assertThat(result).contains("More text")
        assertThat(result).doesNotContain("fun main")
    }

    @Test
    fun `removes inline code`() {
        val content = "Use `println()` to print"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("Use  to print")
    }

    @Test
    fun `removes images`() {
        val content = "Before ![alt text](image.png) after"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("Before  after")
    }

    @Test
    fun `replaces wiki links with display text`() {
        val content = "See [[Some Page|display text]] for details"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("See display text for details")
    }

    @Test
    fun `replaces wiki links without pipe with link name`() {
        val content = "See [[Some Page]] for details"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("See Some Page for details")
    }

    @Test
    fun `replaces markdown links with display text`() {
        val content = "Check [this link](https://example.com) here"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("Check this link here")
    }

    @Test
    fun `removes HTML tags`() {
        val content = "<div>Hello <b>world</b></div>"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("Hello world")
    }

    @Test
    fun `removes heading markers`() {
        val content = "## My Heading"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("My Heading")
    }

    @Test
    fun `removes bold and italic markers`() {
        val content = "This is ***bold italic*** and **bold** and *italic*"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("This is bold italic and bold and italic")
    }

    @Test
    fun `removes strikethrough`() {
        val content = "This is ~~deleted~~ text"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("This is deleted text")
    }

    @Test
    fun `removes blockquote markers`() {
        val content = "> This is a quote"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("This is a quote")
    }

    @Test
    fun `removes horizontal rules`() {
        val content = "Before\n---\nAfter"
        val result = cleaner.clean(content)
        assertThat(result).contains("Before")
        assertThat(result).contains("After")
    }

    @Test
    fun `removes unordered list markers`() {
        val content = "- Item 1\n* Item 2\n+ Item 3"
        val result = cleaner.clean(content)
        assertThat(result).contains("Item 1")
        assertThat(result).contains("Item 2")
        assertThat(result).contains("Item 3")
        assertThat(result).doesNotContain("-")
        assertThat(result).doesNotContain("*")
        assertThat(result).doesNotContain("+")
    }

    @Test
    fun `removes ordered list markers`() {
        val content = "1. First\n2. Second"
        val result = cleaner.clean(content)
        assertThat(result).contains("First")
        assertThat(result).contains("Second")
    }

    @Test
    fun `removes inline tags`() {
        val content = "Some text #tag1 more #tag2/sub"
        val result = cleaner.clean(content)
        assertThat(result).doesNotContain("#tag1")
        assertThat(result).doesNotContain("#tag2/sub")
        assertThat(result).contains("Some text")
    }

    @Test
    fun `collapses multiple blank lines`() {
        val content = "Line 1\n\n\n\n\nLine 2"
        val result = cleaner.clean(content)
        assertThat(result).isEqualTo("Line 1\n\nLine 2")
    }

    @Test
    fun `handles empty content`() {
        val result = cleaner.clean("")
        assertThat(result).isEmpty()
    }

    @Test
    fun `handles content with only frontmatter`() {
        val content = "---\ntitle: Test\n---\n"
        val result = cleaner.clean(content)
        assertThat(result).isEmpty()
    }
}
