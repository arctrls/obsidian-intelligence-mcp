package com.jazzbach.obsidianintelligence.shared

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class MarkdownWriter {

    private val yamlMapper = ObjectMapper(
        YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    ).registerKotlinModule()

    fun updateFrontmatter(path: Path, updates: Map<String, Any?>) {
        val content = Files.readString(path)
        val newContent = updateFrontmatterContent(content, updates)
        Files.writeString(path, newContent)
    }

    fun updateFrontmatterContent(content: String, updates: Map<String, Any?>): String {
        val hasFrontmatter = content.startsWith("---\n") || content.startsWith("---\r\n")

        if (hasFrontmatter) {
            val endIndex = content.indexOf("\n---", 3)
            if (endIndex == -1) return content

            val yamlBlock = content.substring(4, endIndex)
            val existing = try {
                yamlMapper.readValue(yamlBlock, Map::class.java)
                    ?.mapKeys { it.key.toString() }
                    ?.toMutableMap()
                    ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }

            existing.putAll(updates)
            val newYaml = yamlMapper.writeValueAsString(existing).trimEnd()
            val body = content.substring(endIndex + 4)
            return "---\n$newYaml\n---$body"
        } else {
            val newYaml = yamlMapper.writeValueAsString(updates).trimEnd()
            return "---\n$newYaml\n---\n$content"
        }
    }

    fun updateSection(content: String, sectionTitle: String, sectionContent: String): String {
        val sectionPattern = Regex("(?m)^${Regex.escape(sectionTitle)}\\s*\\n(.*?)(?=\\n##\\s|\\Z)", RegexOption.DOT_MATCHES_ALL)
        val match = sectionPattern.find(content)

        return if (match != null) {
            content.replaceRange(match.range, "$sectionTitle\n$sectionContent")
        } else {
            val trimmedContent = content.trimEnd()
            "$trimmedContent\n\n$sectionTitle\n$sectionContent\n"
        }
    }
}
