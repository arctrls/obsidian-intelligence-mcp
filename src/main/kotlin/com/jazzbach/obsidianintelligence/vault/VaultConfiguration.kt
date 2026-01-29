package com.jazzbach.obsidianintelligence.vault

import com.jazzbach.obsidianintelligence.document.ContentCleaner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class VaultConfiguration {

    @Bean
    fun contentCleaner(): ContentCleaner = ContentCleaner()
}
