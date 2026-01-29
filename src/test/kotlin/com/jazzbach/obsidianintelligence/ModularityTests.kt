package com.jazzbach.obsidianintelligence

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTests {

    private val modules = ApplicationModules.of(ObsidianIntelligenceApplication::class.java)

    @Test
    fun `verifies modular structure`() {
        modules.verify()
    }

    @Test
    fun `prints module structure`() {
        modules.forEach(::println)
    }
}
