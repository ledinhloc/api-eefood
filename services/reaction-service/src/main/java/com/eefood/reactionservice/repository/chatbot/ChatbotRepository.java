package com.eefood.reactionservice.repository.chatbot;

import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotRepository extends JpaRepository<ChatbotMessage, Long> {
    List<ChatbotMessage> findTop2ByUserIdAndRoleAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, ChatRole role);
    Optional<ChatbotMessage> findTop1ByUserIdAndRoleAndChatToolAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, ChatRole role, ChatTool chatTool);
    List<ChatbotMessage> findTop2ByUserIdAndRoleAndChatToolAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, ChatRole role, ChatTool chatTool);
    List<ChatbotMessage> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtAsc(Long userId);
}
