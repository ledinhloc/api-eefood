package com.eefood.recipeservice.util;

import com.eefood.recipeservice.dto.response.NutritionAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NutritionSseUtils {
    public void sendStatus(SseEmitter emitter, String msg) {
        sendEvent(emitter, "status", Map.of("message", msg, "data", ""));
    }

    // Gửi phần nutrition tổng + ingredient details (bước 1)
    public void sendNutritionData(SseEmitter emitter, NutritionAnalysisResponse partial) {
        log.info("[SSE] Sending nutrition data for recipe: {}", partial.getRecipeId());
        sendEvent(emitter, "nutrition", Map.of("message", "", "data", partial));
    }

    // Gửi kết quả AI analysis (bước 2: summary, healthLevel, recommendation)
    public void sendAnalysisData(SseEmitter emitter, NutritionAnalysisResponse full) {
        log.info("[SSE] Sending AI analysis for recipe: {}", full.getRecipeId());
        sendEvent(emitter, "analysis", Map.of("message", "", "data", full));
    }

    public void sendError(SseEmitter emitter, String msg) {
        sendEvent(emitter, "error", Map.of("message", msg, "data", List.of()));
    }

    public void sendComplete(SseEmitter emitter, String msg) {
        sendEvent(emitter, "complete", Map.of("message", msg, "data", ""));
    }

    public void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(name)
                            .data(data, MediaType.APPLICATION_JSON)
            );
        } catch (Exception e) {
            log.error("[SSE] Failed to send event '{}': {}", name, e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
