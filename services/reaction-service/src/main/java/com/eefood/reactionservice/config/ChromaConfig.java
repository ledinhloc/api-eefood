package com.eefood.reactionservice.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {
    @Value("${chroma.host}")
    private String host;

    @Value("${chroma.port}")
    private int port;

    @Value("${chroma.collection}")
    private String collection;

    @Bean
    public EmbeddingStore<TextSegment> chromaEmbeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl("http://" + host + ":" + port)
                .collectionName(collection)
                .build();
    }
}
