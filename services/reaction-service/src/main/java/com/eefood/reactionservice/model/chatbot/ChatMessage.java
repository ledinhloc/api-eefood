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
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role;

    @Column(columnDefinition = "text")
    private String inputText;

    private String inputImageUrl;

    @Column(name = "output_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode outputJson;

    @Enumerated(EnumType.STRING)
    private ChatTool chatTool;

    private Integer tokenUsage;
}
