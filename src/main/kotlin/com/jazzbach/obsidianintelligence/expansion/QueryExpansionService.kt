package com.jazzbach.obsidianintelligence.expansion

import org.springframework.stereotype.Service

@Service
class QueryExpansionService : ExpandQuery {

    override fun expand(query: String): ExpandedQuery {
        val tokens = tokenize(query)
        val expandedTerms = mutableSetOf<String>()

        for (token in tokens) {
            val synonyms = KoreanSynonymDictionary.findSynonyms(token)
            expandedTerms.addAll(synonyms)
        }

        expandedTerms.removeAll(tokens.map { it.lowercase() }.toSet())

        val hydeDocument = HydeTemplateGenerator.generate(query, expandedTerms.toList())

        val allTerms = (tokens + expandedTerms).distinct()

        return ExpandedQuery(
            originalQuery = query,
            expandedTerms = expandedTerms.toList(),
            hydeDocument = hydeDocument,
            allTerms = allTerms
        )
    }

    companion object {
        private val TOKEN_PATTERN = Regex("[가-힣a-zA-Z0-9]+")

        fun tokenize(query: String): List<String> {
            return TOKEN_PATTERN.findAll(query).map { it.value }.toList()
        }
    }
}
