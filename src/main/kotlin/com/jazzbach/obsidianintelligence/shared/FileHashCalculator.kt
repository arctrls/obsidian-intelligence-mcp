package com.jazzbach.obsidianintelligence.shared

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@Component
class FileHashCalculator {

    fun calculate(path: Path): String {
        val bytes = Files.readAllBytes(path)
        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
