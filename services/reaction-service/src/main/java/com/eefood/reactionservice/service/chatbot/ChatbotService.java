package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.enums.ErrorMessage;
import com.eefood.reactionservice.model.chatbot.ChatbotMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.service.chatbot.cache.WeatherCacheService;
import com.eefood.reactionservice.util.SseUtils;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    private final ChatbotRepository chatbotRepository;
    private final ChatbotAIService chatbotAIService;
    private final CategoryHintService categoryHintService;
    private final WeatherCacheService weatherCacheService;
    private final Executor asyncExecutor;
    private final SseUtils sseUtils;
    private final ChatbotToolExecutor chatbotToolExecutor;
    private final ChatbotCrudService chatbotCrudService;

    private Pair<String, String> resolveContext(ChatBotRequest request) {

        CompletableFuture<String> weatherFuture =
                CompletableFuture.supplyAsync(() ->
                        weatherCacheService.getWeatherInfo(
                                request.getLocation().getLatitude(),
                                request.getLocation().getLongitude()
                        ), asyncExecutor);

        CompletableFuture<String> categoryFuture =
                weatherFuture.thenApplyAsync(weather ->
                        categoryHintService.generateCategoryHint(
                                request.getMessage(),
                                request.getTime(),
                                weather
                        ), asyncExecutor);

        return Pair.of(weatherFuture.join(), categoryFuture.join());
    }

    public void handleChatStream(ChatBotRequest request, SseEmitter emitter) {
        try {
            processStream(request, emitter);
        } catch (Exception e) {
            handleStreamError(request,emitter, e);

        }
    }

    private void processStream(ChatBotRequest request, SseEmitter emitter) {

        Pair<String,String> ctx = resolveContext(request);

        String userMessage = buildUserMessage(request, ctx.getLeft(), ctx.getRight());

        chatbotCrudService.saveForUserAsync(request);

        startAiStream(userMessage, request, emitter);
    }

    private void startAiStream(
            String userMessage,
            ChatBotRequest request,
            SseEmitter emitter
    ) {


        TokenStream stream = chatbotAIService.chatStream(userMessage);
        ChatbotResponse finalResponse = ChatbotResponse.empty();
        StringBuilder messageBuffer = new StringBuilder();

        stream
                .onPartialResponse(partial -> {
                    log.info("PARTIAL: [{}]", partial);
                    messageBuffer.append(partial);
                    sseUtils.sendMessage(emitter, partial);
                })
                .onToolExecuted(tool -> {
                    try {
                        log.info("TOOL: [{}]", tool.request().name());
                        ChatbotResponse toolResponse = chatbotToolExecutor.execute(tool);
                        finalResponse.setData(toolResponse.getData());
                        finalResponse.setMeta(toolResponse.getMeta());
                        sseUtils.sendData(emitter, toolResponse);
                    } catch (Exception e) {
                        log.error("Tool execution error", e);
                        sseUtils.sendError(emitter, ErrorMessage.AI_NOT_EXCUTED.getMessage());
                    }
                })
                .onCompleteResponse(r -> {
                    finalResponse.setMessage(messageBuffer.toString());
                    sseUtils.sendFinal(emitter, finalResponse);
                    emitter.complete();
                    chatbotCrudService.saveForAIAsync(request, finalResponse);
                })
                .onError(e -> {
                    handleStreamError(request,emitter, e);
                })
                .start();
    }

    // Get 2 chat history
    private String getChatHistory(Long userId) {
        List<ChatbotMessage> history = chatbotRepository.findTop2ByUserIdAndRoleAndIsDeletedFalseOrderByCreatedAtDesc(userId, ChatRole.USER);
        if(history.isEmpty()) {
            return "";
        }
        return history.stream()
                .map(h -> h.getRole() + ": " + h.getMessage())
                .collect(Collectors.joining("\n"));
    }

    private String getAiChatHistory(Long userId) {
        List<ChatbotMessage> aiHistory = chatbotRepository.findTop2ByUserIdAndRoleAndChatToolAndIsDeletedFalseOrderByCreatedAtDesc(userId, ChatRole.AI, ChatTool.SUGGEST_POST);
        if(aiHistory.isEmpty()) {
            return "";
        }
        return aiHistory
                .stream()
                .map(h -> {
                    String content = "";
                    if(h.getData()!=null && !h.getData().isNull())
                    {
                        content = h.getData().toString();
                    }
                    return h.getRole() + ": " + content;
                }).collect(Collectors.joining("\n"));
    }

    // Build user input
    private String buildUserMessage(
            ChatBotRequest request,
            String weather,
            String categoryHint
    ) {
        StringBuilder sb = new StringBuilder();

        // Câu hỏi
        if (isNotBlank(request.getMessage())) {
            sb.append("CÂU HỎI NGƯỜI DÙNG:\n")
                    .append(request.getMessage())
                    .append("\n\n");
        }

        // Thông tin ngữ cảnh
        boolean hasContext =
                isNotBlank(request.getLocation().getProvince()) ||
                        isNotBlank(weather) ||
                        isNotBlank(request.getTime());

        if (hasContext) {
            sb.append("THÔNG TIN NGỮ CẢNH:\n");

            if (isNotBlank(request.getLocation().getProvince())) {
                sb.append("- Địa điểm hiện tại: ")
                        .append(request.getLocation().getProvince())
                        .append("\n");
            }

            if (isNotBlank(weather)) {
                sb.append("- Thời tiết hiện tại: ")
                        .append(weather)
                        .append("\n");
            }

            if (isNotBlank(request.getTime())) {
                sb.append("- Thời gian: ")
                        .append(request.getTime())
                        .append("\n");
            }

            sb.append("\n");
        }

        // Gợi ý danh mục
        if (isNotBlank(categoryHint)) {
            sb.append("GỢI Ý DANH MỤC HỆ THỐNG:\n")
                    .append(categoryHint)
                    .append("\n\n");
        }

        // Lịch sử hội thoại
        String chatHistory = getChatHistory(request.getUserId());
        if (isNotBlank(chatHistory)) {
            sb.append("LỊCH SỬ HỘI THOẠI:\n")
                    .append(chatHistory)
                    .append("\n\n");
        }

        // Lịch sử AI
        String aiHistory = getAiChatHistory(request.getUserId());
        if (isNotBlank(aiHistory)) {
            sb.append("LỊCH SỬ AI:\n")
                    .append(aiHistory)
                    .append("\n\n");
        }

        // Hình ảnh
        if (isNotBlank(request.getImageUrl())) {
            sb.append("HÌNH ẢNH:\n")
                    .append(request.getImageUrl())
                    .append("\n\n");
        }

        // UserId
        if (request.getUserId() != null) {
            sb.append("THÔNG TIN NGƯỜI DÙNG:\n")
                    .append(request.getUserId())
                    .append("\n\n");
        }

        // Post / Recipe
        if (!request.getPostId().isEmpty() || !request.getRecipeId().isEmpty() ) {
            sb.append("THÔNG TIN BÀI VIẾT:\n");

            if (!request.getPostId().isEmpty()) {
                sb.append("- ID BÀI VIẾT: ")
                        .append(request.getPostId())
                        .append("\n");
            }

            if (!request.getRecipeId().isEmpty()) {
                sb.append("- ID CÔNG THỨC: ")
                        .append(request.getRecipeId())
                        .append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    // Handle error like rate limit, overload,...
    private boolean isQuotaExceeded(Throwable e) {
        return containsAny(e,
                "quota",
                "free tier",
                "exceeded your current quota",
                "billing"
        );
    }

    private boolean isOverloaded(Throwable e) {
        return containsAny(e,
                "overloaded",
                "resource_exhausted",
                "temporarily unavailable"
        );
    }

    private boolean isRateLimit(Throwable e) {
        return containsAny(e,
                "429",
                "rate limit"
        );
    }

    private boolean containsAny(Throwable e, String... keywords) {
        Throwable cause = e;

        while (cause != null) {
            String msg = cause.getMessage();
            if (msg != null) {
                msg = msg.toLowerCase();
                for (String keyword : keywords) {
                    if (msg.contains(keyword)) {
                        return true;
                    }
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private ChatbotResponse buildErrorResponse(String message) {
        return ChatbotResponse.builder()
                .message(message)
                .role(ChatRole.AI.name())
                .data(List.of())
                .meta(Map.of(
                        "tool", "NONE",
                        "message", message
                ))
                .build();
    }

    private void handleStreamError(
            ChatBotRequest request,
            SseEmitter emitter,
            Throwable e
    ) {
        try {

            String message;

            if (isQuotaExceeded(e)) {
                log.warn("AI quota exceeded");
                message = ErrorMessage.AI_FREE_QUOTA_EXCEEDED.getMessage();
            }
            else if (isOverloaded(e) || isRateLimit(e)) {
                log.warn("AI overloaded or rate limited");
                message = ErrorMessage.AI_OVERLOADED.getMessage();
            }
            else {
                log.error("AI internal error", e);
                message = ErrorMessage.AI_INTERNAL_ERROR.getMessage();
            }

            ChatbotResponse fallbackResponse = buildErrorResponse(message);
            chatbotCrudService.saveForAIAsync(request, fallbackResponse);

            sseUtils.sendError(emitter, message);
            emitter.complete();

        } catch (Exception ex) {
            log.error("Failed to send error event", ex);
            emitter.completeWithError(ex);
        }
    }
}
