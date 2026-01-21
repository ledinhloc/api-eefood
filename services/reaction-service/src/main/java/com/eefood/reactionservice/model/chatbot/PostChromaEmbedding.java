package com.eefood.reactionservice.model.chatbot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_chroma_embedding")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PostChromaEmbedding {
    @Id
    private Long postId;

    @Column(nullable = false)
    private String chromaEmbeddingId;

    @Column(nullable = false, length = 64)
    private String contentHash;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
