package com.jazzbach.obsidianintelligence.embedding

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.transformers.TransformersEmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.DefaultResourceLoader
import java.lang.reflect.Field
import java.nio.file.Path

@Configuration
class EmbeddingConfiguration {

    @Bean
    fun embeddingModel(
        @Value("\${spring.ai.embedding.transformer.onnx.modelUri}") modelUri: String,
        @Value("\${spring.ai.embedding.transformer.tokenizer.uri}") tokenizerUri: String,
    ): EmbeddingModel {
        val model = TransformersEmbeddingModel()

        val resourceLoader = DefaultResourceLoader()
        val tokenizerResource = resourceLoader.getResource(tokenizerUri)

        val tokenizer = HuggingFaceTokenizer.newInstance(
            tokenizerResource.inputStream,
            mapOf("padding" to "true", "truncation" to "true")
        )

        val environment = OrtEnvironment.getEnvironment()
        val modelPath = Path.of(modelUri.removePrefix("file:")).toAbsolutePath().toString()
        val session = environment.createSession(modelPath, OrtSession.SessionOptions())

        setField(model, "tokenizer", tokenizer)
        setField(model, "environment", environment)
        setField(model, "session", session)
        setField(model, "modelOutputName", "last_hidden_state")
        setField(model, "onnxModelInputs", session.inputNames)

        return model
    }

    private fun setField(obj: Any, name: String, value: Any) {
        val field: Field = obj.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(obj, value)
    }
}
