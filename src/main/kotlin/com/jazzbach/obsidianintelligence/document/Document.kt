package com.jazzbach.obsidianintelligence.document

import java.nio.file.Path
import java.time.Instant

data class Document(
    val path: Path,
    val title: String,
    val content: String,
    val cleanedContent: String,
    val frontmatter: Frontmatter,
    val tags: List<String>,
    val wordCount: Int,
    val fileHash: String,
    val modifiedAt: Instant
)
