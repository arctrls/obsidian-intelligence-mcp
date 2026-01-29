package com.jazzbach.obsidianintelligence.related

import com.jazzbach.obsidianintelligence.shared.MarkdownWriter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class RelatedSectionUpdaterTest {

    @TempDir
    lateinit var tempDir: Path

    private val findRelatedDocuments = mockk<FindRelatedDocuments>()
    private val markdownWriter = MarkdownWriter()
    private val properties = RelatedDocsProperties(
        defaultTopK = 5,
        sectionTitle = "## 관련 문서",
        similarityThreshold = 0.3
    )

    private lateinit var updater: RelatedSectionUpdater

    @BeforeEach
    fun setUp() {
        updater = RelatedSectionUpdater(
            findRelatedDocuments = findRelatedDocuments,
            markdownWriter = markdownWriter,
            relatedDocsProperties = properties
        )
    }

    @Test
    fun `adds related section to document`() {
        val file = createFile("note.md", "# My Note\n\nSome content")
        val related = listOf(
            RelatedDocResult("/vault/related1.md", "Related One", 0.85, listOf("kotlin")),
            RelatedDocResult("/vault/related2.md", "Related Two", 0.72, listOf("spring"))
        )

        every { findRelatedDocuments.findRelated(file.toString(), 5) } returns related

        val result = updater.update(file.toString())

        assertThat(result.success).isTrue()
        assertThat(result.sectionAdded).isTrue()
        assertThat(result.relatedDocs).hasSize(2)

        val updatedContent = Files.readString(file)
        assertThat(updatedContent).contains("## 관련 문서")
        assertThat(updatedContent).contains("Related One")
        assertThat(updatedContent).contains("Related Two")
        assertThat(updatedContent).contains("유사도:")
    }

    @Test
    fun `does not add section when no related docs found`() {
        val file = createFile("isolated.md", "# Isolated Note\n\nContent")

        every { findRelatedDocuments.findRelated(file.toString(), 5) } returns emptyList()

        val result = updater.update(file.toString())

        assertThat(result.success).isTrue()
        assertThat(result.sectionAdded).isFalse()

        val content = Files.readString(file)
        assertThat(content).doesNotContain("## 관련 문서")
    }

    @Test
    fun `handles error gracefully`() {
        val filePath = "/nonexistent/note.md"

        every { findRelatedDocuments.findRelated(filePath, 5) } throws RuntimeException("IO error")

        val result = updater.update(filePath)

        assertThat(result.success).isFalse()
        assertThat(result.errorMessage).isNotNull()
    }

    @Test
    fun `updates existing related section`() {
        val file = createFile("note.md", """
            |# My Note
            |
            |Some content
            |
            |## 관련 문서
            |- [[old-note|Old Note]]
        """.trimMargin())

        val related = listOf(
            RelatedDocResult("/vault/new-note.md", "New Note", 0.90, emptyList())
        )

        every { findRelatedDocuments.findRelated(file.toString(), 5) } returns related

        val result = updater.update(file.toString())

        assertThat(result.success).isTrue()
        val content = Files.readString(file)
        assertThat(content).contains("New Note")
    }

    private fun createFile(name: String, content: String): Path {
        val file = tempDir.resolve(name)
        Files.writeString(file, content)
        return file
    }
}
