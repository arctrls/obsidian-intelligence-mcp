package com.jazzbach.obsidianintelligence.document

import java.nio.file.Path

interface DocumentParser {
    fun parse(path: Path): Document
}
