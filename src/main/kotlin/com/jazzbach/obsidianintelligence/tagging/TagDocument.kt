package com.jazzbach.obsidianintelligence.tagging

interface TagDocument {
    fun tag(filePath: String, dryRun: Boolean = false): TaggingResult
}
