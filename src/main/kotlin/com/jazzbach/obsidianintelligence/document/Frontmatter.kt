package com.jazzbach.obsidianintelligence.document

data class Frontmatter(
    val title: String? = null,
    val tags: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val properties: Map<String, Any?> = emptyMap()
)
