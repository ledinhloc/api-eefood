package com.eefood.reactionservice.repository.chatbot;

import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatbotRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop2ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
    List<ChatMessage> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}
