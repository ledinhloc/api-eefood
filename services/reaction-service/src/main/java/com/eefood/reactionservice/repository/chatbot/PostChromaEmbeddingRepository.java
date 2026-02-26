package com.eefood.reactionservice.repository.chatbot;

import com.eefood.reactionservice.model.chatbot.PostChromaEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostChromaEmbeddingRepository extends JpaRepository<PostChromaEmbedding, Long> {
}
