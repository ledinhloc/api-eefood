package com.eefood.reactionservice.service.chatbot.cache;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingCacheService {
    private final EmbeddingModel embeddingModel;

    //Cache embedding theo nội dung text
    @Cacheable(
            cacheNames = "rag-embeddings",
            key = "#hash"
    )
    public float[] getOrCreate(String text) {
        log.debug("Embedding cache MISS → creating new embedding");
        return embeddingModel.embed(text).content().vector();
    }

    public float[] getOrCreateSafe(String text) {
        String hash = hash(text);
        return getOrCreateInternal(text, hash);
    }

    @Cacheable(
            cacheNames = "rag-embeddings",
            key = "#hash"
    )
    public float[] getOrCreateInternal(String text, String hash) {
        log.debug("Embedding cache MISS → creating new embedding");
        return embeddingModel.embed(text).content().vector();
    }

    // Hash text để làm cache key (tránh key dài)
    public String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }
}
