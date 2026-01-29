package com.jazzbach.obsidianintelligence.topic

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class TopicMcpTool(
    private val collectTopic: CollectTopic,
    private val topicProperties: TopicProperties
) {

    @Tool(
        name = "collect-topic",
        description = "Collect and organize documents related to a specific topic from the Obsidian vault. " +
                "Groups documents by tags, provides statistics (word counts, tag frequency), " +
                "and suggests related topics based on tag co-occurrence."
    )
    fun collectTopicDocuments(
        @ToolParam(description = "The topic to search for and collect documents about.")
        topic: String,
        @ToolParam(description = "Maximum number of documents to collect. Default is 50.", required = false)
        topK: Int?,
        @ToolParam(description = "Minimum similarity threshold (0.0 to 1.0). Default is 0.3.", required = false)
        similarityThreshold: Double?,
        @ToolParam(description = "Minimum word count filter. Documents below this count are excluded.", required = false)
        minWordCount: Int?,
        @ToolParam(description = "Comma-separated list of tags to filter by.", required = false)
        filterTags: String?
    ): TopicToolResponse {
        val request = TopicRequest(
            topic = topic,
            topK = topK ?: topicProperties.defaultTopK,
            similarityThreshold = similarityThreshold ?: topicProperties.similarityThreshold,
            minWordCount = minWordCount ?: 0,
            filterTags = filterTags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        )

        val collection = collectTopic.collect(request)

        return TopicToolResponse(
            topic = collection.topic,
            totalDocuments = collection.totalDocuments,
            groups = collection.groups.map { group ->
                TopicGroupResult(
                    tagName = group.tagName,
                    documentCount = group.documents.size,
                    documents = group.documents.map { doc ->
                        TopicDocumentResult(
                            filePath = doc.filePath,
                            title = doc.title,
                            score = "%.4f".format(doc.score),
                            tags = doc.tags,
                            wordCount = doc.wordCount,
                            snippet = doc.snippet
                        )
                    }
                )
            },
            statistics = TopicStatisticsResult(
                minWordCount = collection.statistics.minWordCount,
                maxWordCount = collection.statistics.maxWordCount,
                avgWordCount = "%.1f".format(collection.statistics.avgWordCount),
                topTags = collection.statistics.tagFrequency.map { (tag, count) ->
                    TagFrequencyResult(tag = tag, count = count)
                }
            ),
            relatedTopics = collection.relatedTopics
        )
    }
}

data class TopicToolResponse(
    val topic: String,
    val totalDocuments: Int,
    val groups: List<TopicGroupResult>,
    val statistics: TopicStatisticsResult,
    val relatedTopics: List<String>
)

data class TopicGroupResult(
    val tagName: String,
    val documentCount: Int,
    val documents: List<TopicDocumentResult>
)

data class TopicDocumentResult(
    val filePath: String,
    val title: String,
    val score: String,
    val tags: List<String>,
    val wordCount: Int,
    val snippet: String
)

data class TopicStatisticsResult(
    val minWordCount: Int,
    val maxWordCount: Int,
    val avgWordCount: String,
    val topTags: List<TagFrequencyResult>
)

data class TagFrequencyResult(
    val tag: String,
    val count: Int
)
