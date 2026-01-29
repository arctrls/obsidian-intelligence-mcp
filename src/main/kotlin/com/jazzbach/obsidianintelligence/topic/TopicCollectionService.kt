package com.jazzbach.obsidianintelligence.topic

import com.jazzbach.obsidianintelligence.search.SearchDocuments
import com.jazzbach.obsidianintelligence.search.SearchQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TopicCollectionService(
    private val searchDocuments: SearchDocuments,
    private val topicProperties: TopicProperties
) : CollectTopic {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun collect(request: TopicRequest): TopicCollection {
        val topK = if (request.topK > 0) request.topK else topicProperties.defaultTopK
        val threshold = if (request.similarityThreshold > 0) request.similarityThreshold
        else topicProperties.similarityThreshold

        log.info("Collecting topic '{}' with topK={}", request.topic, topK)

        val searchQuery = SearchQuery(
            text = request.topic,
            topK = topK,
            similarityThreshold = threshold,
            tags = request.filterTags
        )

        val results = searchDocuments.search(searchQuery)

        val filtered = results.filter { result ->
            request.minWordCount <= 0 || result.wordCount >= request.minWordCount
        }

        val topicDocuments = filtered.map { result ->
            TopicDocument(
                filePath = result.filePath,
                title = result.title,
                score = result.score,
                tags = result.tags,
                wordCount = result.wordCount,
                snippet = result.snippet
            )
        }

        val tagCounter = mutableMapOf<String, Int>()
        for (doc in topicDocuments) {
            for (tag in doc.tags) {
                tagCounter[tag] = (tagCounter[tag] ?: 0) + 1
            }
        }

        val groups = groupByTag(topicDocuments, tagCounter)
        val statistics = calculateStatistics(topicDocuments, tagCounter)
        val relatedTopics = findRelatedTopics(tagCounter)

        log.info("Collected {} documents in {} groups for topic '{}'",
            topicDocuments.size, groups.size, request.topic)

        return TopicCollection(
            topic = request.topic,
            totalDocuments = topicDocuments.size,
            groups = groups,
            statistics = statistics,
            relatedTopics = relatedTopics
        )
    }

    private fun groupByTag(
        documents: List<TopicDocument>,
        tagCounter: Map<String, Int>
    ): List<TopicGroup> {
        val minGroupSize = topicProperties.minGroupSize
        val significantTags = tagCounter.filter { it.value >= minGroupSize }.keys

        val groups = mutableListOf<TopicGroup>()
        val assigned = mutableSetOf<String>()

        for (tag in significantTags.sortedByDescending { tagCounter[it] ?: 0 }) {
            val docsForTag = documents.filter { doc ->
                doc.filePath !in assigned && doc.tags.contains(tag)
            }
            if (docsForTag.size >= minGroupSize) {
                groups.add(TopicGroup(tagName = tag, documents = docsForTag))
                docsForTag.forEach { assigned.add(it.filePath) }
            }
        }

        val unassigned = documents.filter { it.filePath !in assigned }
        if (unassigned.isNotEmpty()) {
            groups.add(TopicGroup(tagName = "기타", documents = unassigned))
        }

        return groups
    }

    private fun calculateStatistics(
        documents: List<TopicDocument>,
        tagCounter: Map<String, Int>
    ): TopicStatistics {
        val wordCounts = documents.map { it.wordCount }
        val topTags = tagCounter.entries
            .sortedByDescending { it.value }
            .take(20)
            .map { it.key to it.value }

        return TopicStatistics(
            minWordCount = wordCounts.minOrNull() ?: 0,
            maxWordCount = wordCounts.maxOrNull() ?: 0,
            avgWordCount = if (wordCounts.isNotEmpty()) wordCounts.average() else 0.0,
            tagFrequency = topTags
        )
    }

    private fun findRelatedTopics(tagCounter: Map<String, Int>): List<String> {
        return tagCounter.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
}
