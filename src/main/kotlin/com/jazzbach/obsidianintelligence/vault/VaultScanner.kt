package com.jazzbach.obsidianintelligence.vault

import java.nio.file.Path

interface VaultScanner {
    fun findAllFiles(): List<Path>
}
