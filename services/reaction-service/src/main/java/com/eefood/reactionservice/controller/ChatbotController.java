package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.service.chatbot.ChatbotCrudService;
import com.eefood.reactionservice.service.chatbot.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private final ChatbotService chatbotService;
    private final ChatbotCrudService chatbotCrudService;

    @PostMapping
    public ResponseData<ChatbotResponse> chat(@RequestBody ChatBotRequest request) {
        return new ResponseData<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                chatbotService.handleChat(request)
        );
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatbotStream(@RequestBody ChatBotRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);

        chatbotService.handleChatStream(request, emitter);

        return emitter;
    }

    @GetMapping("/{userId}")
    public ResponseData<List<ChatbotResponse>> getListChatHistory(@PathVariable Long userId) {
        return new ResponseData<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.getReasonPhrase(),
                chatbotCrudService.getListChatbotHistory(userId)
        );
    }
}
