package com.jazzbach.obsidianintelligence.document

class ContentCleaner {

    fun clean(content: String): String {
        var cleaned = content

        // Remove YAML frontmatter
        cleaned = FRONTMATTER_PATTERN.replace(cleaned, "")

        // Remove code blocks (fenced)
        cleaned = CODE_BLOCK_PATTERN.replace(cleaned, "")

        // Remove inline code
        cleaned = INLINE_CODE_PATTERN.replace(cleaned, "")

        // Remove images
        cleaned = IMAGE_PATTERN.replace(cleaned, "")

        // Remove wiki-style links, keeping display text
        cleaned = WIKI_LINK_DISPLAY_PATTERN.replace(cleaned, "$1")
        cleaned = WIKI_LINK_PATTERN.replace(cleaned, "$1")

        // Remove markdown links, keeping display text
        cleaned = MD_LINK_PATTERN.replace(cleaned, "$1")

        // Remove HTML tags
        cleaned = HTML_TAG_PATTERN.replace(cleaned, "")

        // Remove heading markers
        cleaned = HEADING_PATTERN.replace(cleaned, "$1")

        // Remove bold/italic markers
        cleaned = BOLD_ITALIC_PATTERN.replace(cleaned, "$1")
        cleaned = BOLD_PATTERN.replace(cleaned, "$1")
        cleaned = ITALIC_PATTERN.replace(cleaned, "$1")

        // Remove strikethrough
        cleaned = STRIKETHROUGH_PATTERN.replace(cleaned, "$1")

        // Remove blockquote markers
        cleaned = BLOCKQUOTE_PATTERN.replace(cleaned, "$1")

        // Remove horizontal rules
        cleaned = HR_PATTERN.replace(cleaned, "")

        // Remove list markers (-, *, +, 1.)
        cleaned = UNORDERED_LIST_PATTERN.replace(cleaned, "$1")
        cleaned = ORDERED_LIST_PATTERN.replace(cleaned, "$1")

        // Remove inline tags (#tag)
        cleaned = INLINE_TAG_PATTERN.replace(cleaned, "")

        // Collapse multiple blank lines
        cleaned = MULTIPLE_BLANK_LINES.replace(cleaned, "\n\n")

        return cleaned.trim()
    }

    companion object {
        private val FRONTMATTER_PATTERN = Regex("(?s)\\A---\\n.*?\\n---\\n?")
        private val CODE_BLOCK_PATTERN = Regex("(?s)```.*?```")
        private val INLINE_CODE_PATTERN = Regex("`[^`]+`")
        private val IMAGE_PATTERN = Regex("!\\[[^]]*]\\([^)]*\\)")
        private val WIKI_LINK_DISPLAY_PATTERN = Regex("\\[\\[[^]]*\\|([^]]+)]]")
        private val WIKI_LINK_PATTERN = Regex("\\[\\[([^]]+)]]")
        private val MD_LINK_PATTERN = Regex("\\[([^]]+)]\\([^)]*\\)")
        private val HTML_TAG_PATTERN = Regex("<[^>]+>")
        private val HEADING_PATTERN = Regex("(?m)^#{1,6}\\s+(.*)")
        private val BOLD_ITALIC_PATTERN = Regex("\\*{3}(.+?)\\*{3}")
        private val BOLD_PATTERN = Regex("\\*{2}(.+?)\\*{2}")
        private val ITALIC_PATTERN = Regex("\\*(.+?)\\*")
        private val STRIKETHROUGH_PATTERN = Regex("~~(.+?)~~")
        private val BLOCKQUOTE_PATTERN = Regex("(?m)^>\\s?(.*)")
        private val HR_PATTERN = Regex("(?m)^[-*_]{3,}\\s*$")
        private val UNORDERED_LIST_PATTERN = Regex("(?m)^(\\s*)[-*+]\\s+")
        private val ORDERED_LIST_PATTERN = Regex("(?m)^(\\s*)\\d+\\.\\s+")
        private val INLINE_TAG_PATTERN = Regex("(?<=\\s|^)#[a-zA-Z][\\w/-]*")
        private val MULTIPLE_BLANK_LINES = Regex("\\n{3,}")
    }
}
