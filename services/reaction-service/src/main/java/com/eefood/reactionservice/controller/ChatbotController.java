package com.eefood.reactionservice.controller;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.service.chatbot.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private final ChatbotService chatbotService;

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
}
