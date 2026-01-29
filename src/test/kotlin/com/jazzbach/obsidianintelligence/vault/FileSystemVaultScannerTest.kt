package com.jazzbach.obsidianintelligence.vault

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileSystemVaultScannerTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // Create vault structure
        Files.createDirectories(tempDir.resolve("notes"))
        Files.createDirectories(tempDir.resolve("projects"))
        Files.createDirectories(tempDir.resolve(".obsidian"))
        Files.createDirectories(tempDir.resolve(".trash"))
        Files.createDirectories(tempDir.resolve("ATTACHMENTS"))
    }

    @Test
    fun `finds markdown files in vault`() {
        Files.writeString(tempDir.resolve("notes/note1.md"), "Note 1")
        Files.writeString(tempDir.resolve("projects/project.md"), "Project")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files).hasSize(2)
    }

    @Test
    fun `excludes configured directories`() {
        Files.writeString(tempDir.resolve("notes/note1.md"), "Note")
        Files.writeString(tempDir.resolve(".obsidian/config.md"), "Config")
        Files.writeString(tempDir.resolve(".trash/deleted.md"), "Deleted")
        Files.writeString(tempDir.resolve("ATTACHMENTS/file.md"), "Attachment")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files).hasSize(1)
        assertThat(files[0].fileName.toString()).isEqualTo("note1.md")
    }

    @Test
    fun `only includes configured file extensions`() {
        Files.writeString(tempDir.resolve("notes/note.md"), "Note")
        Files.writeString(tempDir.resolve("notes/readme.txt"), "Readme")
        Files.writeString(tempDir.resolve("notes/doc.markdown"), "Doc")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files).hasSize(2)
        assertThat(files.map { it.fileName.toString() })
            .containsExactlyInAnyOrder("note.md", "doc.markdown")
    }

    @Test
    fun `returns sorted list`() {
        Files.writeString(tempDir.resolve("notes/c.md"), "C")
        Files.writeString(tempDir.resolve("notes/a.md"), "A")
        Files.writeString(tempDir.resolve("notes/b.md"), "B")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files.map { it.fileName.toString() })
            .containsExactly("a.md", "b.md", "c.md")
    }

    @Test
    fun `returns empty list for non-existent vault`() {
        val scanner = FileSystemVaultScanner(
            VaultProperties(path = "/non/existent/path")
        )

        val files = scanner.findAllFiles()

        assertThat(files).isEmpty()
    }

    @Test
    fun `scans nested directories`() {
        Files.createDirectories(tempDir.resolve("notes/sub/deep"))
        Files.writeString(tempDir.resolve("notes/sub/deep/nested.md"), "Nested")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files).hasSize(1)
        assertThat(files[0].fileName.toString()).isEqualTo("nested.md")
    }

    @Test
    fun `excludes subdirectory of excluded dir`() {
        Files.createDirectories(tempDir.resolve(".obsidian/plugins"))
        Files.writeString(tempDir.resolve(".obsidian/plugins/plugin.md"), "Plugin")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files).isEmpty()
    }

    @Test
    fun `finds files in root of vault`() {
        Files.writeString(tempDir.resolve("root-note.md"), "Root")

        val scanner = createScanner()
        val files = scanner.findAllFiles()

        assertThat(files).hasSize(1)
    }

    private fun createScanner(): FileSystemVaultScanner {
        return FileSystemVaultScanner(
            VaultProperties(
                path = tempDir.toString(),
                excludedDirs = listOf(".obsidian", ".trash", ".git", "ATTACHMENTS"),
                fileExtensions = listOf(".md", ".markdown")
            )
        )
    }
}
