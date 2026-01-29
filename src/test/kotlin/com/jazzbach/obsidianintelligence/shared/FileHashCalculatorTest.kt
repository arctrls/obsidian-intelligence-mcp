package com.jazzbach.obsidianintelligence.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileHashCalculatorTest {

    @TempDir
    lateinit var tempDir: Path

    private val calculator = FileHashCalculator()

    @Test
    fun `produces 32 character hex string`() {
        val file = tempDir.resolve("test.txt")
        Files.writeString(file, "Hello")

        val hash = calculator.calculate(file)

        assertThat(hash).hasSize(32)
        assertThat(hash).matches("[0-9a-f]{32}")
    }

    @Test
    fun `same content produces same hash`() {
        val file1 = tempDir.resolve("a.txt")
        val file2 = tempDir.resolve("b.txt")
        Files.writeString(file1, "Same content")
        Files.writeString(file2, "Same content")

        assertThat(calculator.calculate(file1)).isEqualTo(calculator.calculate(file2))
    }

    @Test
    fun `different content produces different hash`() {
        val file1 = tempDir.resolve("a.txt")
        val file2 = tempDir.resolve("b.txt")
        Files.writeString(file1, "Content A")
        Files.writeString(file2, "Content B")

        assertThat(calculator.calculate(file1)).isNotEqualTo(calculator.calculate(file2))
    }

    @Test
    fun `handles empty file`() {
        val file = tempDir.resolve("empty.txt")
        Files.writeString(file, "")

        val hash = calculator.calculate(file)

        assertThat(hash).hasSize(32)
    }
}
