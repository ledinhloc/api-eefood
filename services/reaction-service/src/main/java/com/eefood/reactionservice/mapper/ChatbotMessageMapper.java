package com.eefood.reactionservice.mapper;

import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.model.chatbot.ChatbotMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ChatbotMessageMapper {

    ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(source = "role", target = "role")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "data", target = "data")
    @Mapping(source = "meta", target = "meta")
    ChatbotResponse toResponse(ChatbotMessage entity);

    default List<Object> mapData(JsonNode node) {
        if (node == null || node.isNull()) return new ArrayList<>();
        return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
    }

    default Map<String, Object> mapMeta(JsonNode node) {
        if (node == null || node.isNull()) return new HashMap<>();
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    default String map(ChatRole role) {
        return role == null ? null : role.name();
    }
}
