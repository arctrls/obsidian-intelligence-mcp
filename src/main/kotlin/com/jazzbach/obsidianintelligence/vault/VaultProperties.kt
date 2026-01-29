package com.jazzbach.obsidianintelligence.vault

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "obsidian-intelligence.vault")
data class VaultProperties(
    val path: String = "/vault",
    val excludedDirs: List<String> = listOf(".obsidian", ".trash", ".git", "ATTACHMENTS"),
    val fileExtensions: List<String> = listOf(".md", ".markdown")
)
