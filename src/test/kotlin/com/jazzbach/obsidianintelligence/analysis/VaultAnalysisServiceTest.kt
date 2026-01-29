package com.jazzbach.obsidianintelligence.analysis

import com.jazzbach.obsidianintelligence.document.Document
import com.jazzbach.obsidianintelligence.document.DocumentParser
import com.jazzbach.obsidianintelligence.document.Frontmatter
import com.jazzbach.obsidianintelligence.tagging.DefaultTagRuleEngine
import com.jazzbach.obsidianintelligence.tagging.TaggingProperties
import com.jazzbach.obsidianintelligence.vault.VaultScanner
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant

class VaultAnalysisServiceTest {

    private val vaultScanner = mockk<VaultScanner>()
    private val documentParser = mockk<DocumentParser>()
    private val tagRuleEngine = DefaultTagRuleEngine(TaggingProperties())

    private lateinit var service: VaultAnalysisService

    @BeforeEach
    fun setUp() {
        service = VaultAnalysisService(vaultScanner, documentParser, tagRuleEngine)
    }

    @Test
    fun `calculates total file count`() {
        val paths = listOf(
            Path.of("/vault/notes/a.md"),
            Path.of("/vault/notes/b.md"),
            Path.of("/vault/projects/c.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        paths.forEach { path ->
            every { documentParser.parse(path) } returns createDocument(path)
        }

        val stats = service.analyze()

        assertThat(stats.totalFiles).isEqualTo(3)
    }

    @Test
    fun `calculates total and average word count`() {
        val paths = listOf(
            Path.of("/vault/a.md"),
            Path.of("/vault/b.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        every { documentParser.parse(paths[0]) } returns createDocument(paths[0], wordCount = 100)
        every { documentParser.parse(paths[1]) } returns createDocument(paths[1], wordCount = 200)

        val stats = service.analyze()

        assertThat(stats.totalWordCount).isEqualTo(300)
        assertThat(stats.averageWordCount).isEqualTo(150.0)
    }

    @Test
    fun `counts files by directory`() {
        val paths = listOf(
            Path.of("/vault/notes/a.md"),
            Path.of("/vault/notes/b.md"),
            Path.of("/vault/projects/c.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        paths.forEach { path ->
            every { documentParser.parse(path) } returns createDocument(path)
        }

        val stats = service.analyze()

        assertThat(stats.filesByDirectory["notes"]).isEqualTo(2)
        assertThat(stats.filesByDirectory["projects"]).isEqualTo(1)
    }

    @Test
    fun `analyzes tag frequency`() {
        val paths = listOf(
            Path.of("/vault/a.md"),
            Path.of("/vault/b.md"),
            Path.of("/vault/c.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        every { documentParser.parse(paths[0]) } returns createDocument(paths[0], tags = listOf("kotlin", "spring"))
        every { documentParser.parse(paths[1]) } returns createDocument(paths[1], tags = listOf("kotlin", "jpa"))
        every { documentParser.parse(paths[2]) } returns createDocument(paths[2], tags = emptyList())

        val stats = service.analyze()

        assertThat(stats.tagAnalysis.totalUniqueTags).isEqualTo(3)
        assertThat(stats.tagAnalysis.tagFrequency["kotlin"]).isEqualTo(2)
        assertThat(stats.tagAnalysis.tagFrequency["spring"]).isEqualTo(1)
        assertThat(stats.tagAnalysis.untaggedFiles).isEqualTo(1)
    }

    @Test
    fun `returns top tags sorted by frequency`() {
        val paths = listOf(
            Path.of("/vault/a.md"),
            Path.of("/vault/b.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        every { documentParser.parse(paths[0]) } returns createDocument(paths[0], tags = listOf("kotlin", "kotlin"))
        every { documentParser.parse(paths[1]) } returns createDocument(paths[1], tags = listOf("kotlin", "spring"))

        val stats = service.analyze()

        assertThat(stats.tagAnalysis.topTags.first().first).isEqualTo("kotlin")
    }

    @Test
    fun `calculates average tags per file`() {
        val paths = listOf(
            Path.of("/vault/a.md"),
            Path.of("/vault/b.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        every { documentParser.parse(paths[0]) } returns createDocument(paths[0], tags = listOf("kotlin", "spring"))
        every { documentParser.parse(paths[1]) } returns createDocument(paths[1], tags = listOf("jpa"))

        val stats = service.analyze()

        assertThat(stats.tagAnalysis.averageTagsPerFile).isEqualTo(1.5)
    }

    @Test
    fun `handles empty vault`() {
        every { vaultScanner.findAllFiles() } returns emptyList()

        val stats = service.analyze()

        assertThat(stats.totalFiles).isEqualTo(0)
        assertThat(stats.totalWordCount).isEqualTo(0)
        assertThat(stats.averageWordCount).isEqualTo(0.0)
    }

    @Test
    fun `handles unparseable files gracefully`() {
        val paths = listOf(
            Path.of("/vault/good.md"),
            Path.of("/vault/bad.md")
        )

        every { vaultScanner.findAllFiles() } returns paths
        every { documentParser.parse(paths[0]) } returns createDocument(paths[0], wordCount = 100)
        every { documentParser.parse(paths[1]) } throws RuntimeException("Parse error")

        val stats = service.analyze()

        assertThat(stats.totalFiles).isEqualTo(2)
        assertThat(stats.totalWordCount).isEqualTo(100)
    }

    @Test
    fun `provides category distribution`() {
        val paths = listOf(Path.of("/vault/a.md"))

        every { vaultScanner.findAllFiles() } returns paths
        every { documentParser.parse(paths[0]) } returns createDocument(
            paths[0],
            tags = listOf("architecture/design", "spring-boot", "guide")
        )

        val stats = service.analyze()

        assertThat(stats.tagAnalysis.categoryDistribution).isNotEmpty()
    }

    private fun createDocument(
        path: Path,
        wordCount: Int = 50,
        tags: List<String> = emptyList()
    ): Document = Document(
        path = path,
        title = path.fileName.toString().removeSuffix(".md"),
        content = "Test content",
        cleanedContent = "Test content",
        frontmatter = Frontmatter(tags = tags),
        tags = tags,
        wordCount = wordCount,
        fileHash = "hash_${path.fileName}",
        modifiedAt = Instant.now()
    )
}
