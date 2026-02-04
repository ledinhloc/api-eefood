package com.eefood.reactionservice.util;

import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
@Component
public class SseUtils {
    public void sendStatus(SseEmitter emitter, String msg) {
        sendEvent(emitter, "status", Map.of("message", msg, "data", ""));
    }

    public void sendMessage(SseEmitter emitter, String msg) {
        sendEvent(emitter, "message", Map.of("message", msg, "data", ""));
    }

    public void sendData(SseEmitter emitter, Object data) {
        sendEvent(emitter, "data", Map.of("message", "", "data", data));
    }

    public void sendError(SseEmitter emitter, String msg) {
        sendEvent(emitter, "error", Map.of("message", msg, "data", List.of()));
    }

    public void sendFinal(SseEmitter emitter, ChatbotResponse r) {
        sendEvent(emitter, "complete", Map.of(
                "message", r.getMessage(),
                "data", r.getData()
        ));
    }

    public void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(name)
                            .data(data, MediaType.APPLICATION_JSON)
            );
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

}
