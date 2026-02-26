package com.eefood.reactionservice.model.chatbot;

import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.BaseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chatbot_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ChatbotMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(columnDefinition = "text")
    private String message;

    private String imageUrl;

    @Column(name = "data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode data;

    @Column(name = "meta", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode meta;

    @Enumerated(EnumType.STRING)
    private ChatTool chatTool;

    private Integer tokenUsage;
}
