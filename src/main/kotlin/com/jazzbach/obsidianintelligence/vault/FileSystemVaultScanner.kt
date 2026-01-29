package com.jazzbach.obsidianintelligence.vault

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

@Component
class FileSystemVaultScanner(
    private val vaultProperties: VaultProperties
) : VaultScanner {

    override fun findAllFiles(): List<Path> {
        val vaultPath = Path.of(vaultProperties.path)
        if (!Files.isDirectory(vaultPath)) {
            return emptyList()
        }

        val excludedDirs = vaultProperties.excludedDirs.toSet()
        val extensions = vaultProperties.fileExtensions.map { it.removePrefix(".") }.toSet()

        return Files.walk(vaultPath)
            .filter { path -> path.isRegularFile() }
            .filter { path -> hasAllowedExtension(path, extensions) }
            .filter { path -> !isInExcludedDir(path, vaultPath, excludedDirs) }
            .sorted()
            .toList()
    }

    private fun hasAllowedExtension(path: Path, extensions: Set<String>): Boolean {
        return path.extension in extensions
    }

    private fun isInExcludedDir(path: Path, vaultRoot: Path, excludedDirs: Set<String>): Boolean {
        val relativePath = vaultRoot.relativize(path)
        for (i in 0 until relativePath.nameCount - 1) {
            if (relativePath.getName(i).name in excludedDirs) {
                return true
            }
        }
        return false
    }
}
