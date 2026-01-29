package com.jazzbach.obsidianintelligence.topic

interface CollectTopic {
    fun collect(request: TopicRequest): TopicCollection
}
