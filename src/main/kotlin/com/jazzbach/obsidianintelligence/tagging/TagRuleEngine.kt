package com.jazzbach.obsidianintelligence.tagging

interface TagRuleEngine {
    fun normalizeTag(tag: String): String
    fun validateTag(tag: String): Boolean
    fun categorizeTags(tags: List<String>): Map<TagCategory, List<String>>
    fun applyHierarchicalStructure(concepts: List<String>): List<String>
    fun limitTagCount(tags: List<String>, maxCount: Int): List<String>
}
