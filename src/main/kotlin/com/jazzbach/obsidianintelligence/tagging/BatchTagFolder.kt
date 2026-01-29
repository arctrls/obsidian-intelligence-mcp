package com.jazzbach.obsidianintelligence.tagging

interface BatchTagFolder {
    fun tagFolder(folderPath: String, recursive: Boolean = true, dryRun: Boolean = false): List<TaggingResult>
}
