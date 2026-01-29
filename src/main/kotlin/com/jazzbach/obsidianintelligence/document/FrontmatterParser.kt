package com.jazzbach.obsidianintelligence.document

interface FrontmatterParser {
    fun parse(content: String): Frontmatter
    fun hasFrontmatter(content: String): Boolean
    fun extractBody(content: String): String
}
