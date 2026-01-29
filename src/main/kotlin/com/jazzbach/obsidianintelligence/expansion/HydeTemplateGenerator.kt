package com.jazzbach.obsidianintelligence.expansion

object HydeTemplateGenerator {

    private val KOREAN_PATTERN = Regex("[가-힣]")

    fun generate(query: String, synonyms: List<String> = emptyList()): String {
        val isKorean = KOREAN_PATTERN.containsMatchIn(query)
        val allTerms = (listOf(query) + synonyms).distinct()

        return if (isKorean) {
            generateKoreanDocument(query, allTerms)
        } else {
            generateEnglishDocument(query, allTerms)
        }
    }

    private fun generateKoreanDocument(query: String, terms: List<String>): String {
        val termList = terms.joinToString(", ")
        return buildString {
            appendLine("# $query")
            appendLine()
            appendLine("이 문서는 $query 에 대한 내용을 다룹니다.")
            if (terms.size > 1) {
                appendLine("관련 키워드: $termList")
            }
            appendLine("$query 의 주요 개념과 활용 방법을 설명합니다.")
        }.trim()
    }

    private fun generateEnglishDocument(query: String, terms: List<String>): String {
        val termList = terms.joinToString(", ")
        return buildString {
            appendLine("# $query")
            appendLine()
            appendLine("This document covers the topic of $query.")
            if (terms.size > 1) {
                appendLine("Related keywords: $termList")
            }
            appendLine("It explains the key concepts and practical applications of $query.")
        }.trim()
    }
}
