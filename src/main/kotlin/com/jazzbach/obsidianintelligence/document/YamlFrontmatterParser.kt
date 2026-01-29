package com.jazzbach.obsidianintelligence.document

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.stereotype.Component

@Component
class YamlFrontmatterParser : FrontmatterParser {

    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    override fun parse(content: String): Frontmatter {
        if (!hasFrontmatter(content)) {
            return Frontmatter()
        }

        val yamlContent = extractYamlBlock(content) ?: return Frontmatter()

        return try {
            val properties = yamlMapper.readValue(yamlContent, Map::class.java)
                ?.mapKeys { it.key.toString() }
                ?: return Frontmatter()

            val title = properties["title"]?.toString()
            val tags = extractTags(properties)
            val aliases = extractStringList(properties, "aliases")

            Frontmatter(
                title = title,
                tags = tags,
                aliases = aliases,
                properties = properties
            )
        } catch (e: Exception) {
            Frontmatter()
        }
    }

    override fun hasFrontmatter(content: String): Boolean {
        return content.startsWith("---\n") || content.startsWith("---\r\n")
    }

    override fun extractBody(content: String): String {
        if (!hasFrontmatter(content)) {
            return content
        }
        val endIndex = content.indexOf("\n---", 3)
        if (endIndex == -1) {
            return content
        }
        val bodyStart = content.indexOf('\n', endIndex + 3)
        if (bodyStart == -1) {
            return ""
        }
        return content.substring(bodyStart + 1)
    }

    private fun extractYamlBlock(content: String): String? {
        val endIndex = content.indexOf("\n---", 3)
        if (endIndex == -1) return null
        return content.substring(4, endIndex)
    }

    private fun extractTags(properties: Map<String, Any?>): List<String> {
        val tagsValue = properties["tags"] ?: return emptyList()
        return when (tagsValue) {
            is List<*> -> tagsValue.mapNotNull { it?.toString() }
            is String -> tagsValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun extractStringList(properties: Map<String, Any?>, key: String): List<String> {
        val value = properties[key] ?: return emptyList()
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> listOf(value)
            else -> emptyList()
        }
    }
}
