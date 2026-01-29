package com.jazzbach.obsidianintelligence.search

import org.springframework.stereotype.Component

@Component
class KeywordSearchService {

    fun calculateScore(
        title: String,
        tags: List<String>,
        content: String,
        keywords: List<String>
    ): Double {
        if (keywords.isEmpty()) return 0.0

        var totalScore = 0.0
        var matchedKeywordCount = 0

        for (keyword in keywords) {
            val lowerKeyword = keyword.lowercase()
            var keywordScore = 0.0

            if (title.lowercase().contains(lowerKeyword)) {
                keywordScore += TITLE_WEIGHT
            }

            if (tags.any { it.lowercase().contains(lowerKeyword) }) {
                keywordScore += TAG_WEIGHT
            }

            val lowerContent = content.lowercase()
            val frequency = countOccurrences(lowerContent, lowerKeyword)
            if (frequency > 0) {
                keywordScore += (CONTENT_WEIGHT * frequency).coerceAtMost(MAX_CONTENT_SCORE)
            }

            if (keywordScore > 0) {
                matchedKeywordCount++
            }
            totalScore += keywordScore
        }

        val coverageRatio = if (keywords.isNotEmpty()) {
            matchedKeywordCount.toDouble() / keywords.size
        } else 0.0

        return totalScore * coverageRatio
    }

    companion object {
        const val TITLE_WEIGHT = 3.0
        const val TAG_WEIGHT = 2.0
        const val CONTENT_WEIGHT = 1.0
        const val MAX_CONTENT_SCORE = 5.0

        fun countOccurrences(text: String, keyword: String): Int {
            if (keyword.isEmpty()) return 0
            var count = 0
            var startIndex = 0
            while (true) {
                val index = text.indexOf(keyword, startIndex)
                if (index < 0) break
                count++
                startIndex = index + keyword.length
            }
            return count
        }
    }
}
