package com.eefood.reactionservice.repository.chatbot;

import com.eefood.reactionservice.model.chatbot.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotRepository extends JpaRepository<ChatMessage, Long> {
}
