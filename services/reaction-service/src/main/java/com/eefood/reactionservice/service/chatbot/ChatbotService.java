package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatbotMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.service.chatbot.cache.WeatherCacheService;
import com.eefood.reactionservice.util.SseUtils;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
            handleStreamError(emitter, e);
        }
    }

    private void processStream(ChatBotRequest request, SseEmitter emitter) {

        sseUtils.sendStatus(emitter, "Đang xử lý yêu cầu...");

        Pair<String,String> ctx = resolveContext(request);

        String userMessage = buildUserMessage(request, ctx.getLeft(), ctx.getRight());

        chatbotCrudService.saveForUserAsync(request);

        sseUtils.sendStatus(emitter, "Đang phân tích yêu cầu...");

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
                        ChatbotResponse toolResponse = chatbotToolExecutor.execute(tool);
                        finalResponse.setData(toolResponse.getData());
                        finalResponse.setMeta(toolResponse.getMeta());
                        sseUtils.sendData(emitter, toolResponse.getData());
                    } catch (Exception e) {
                        log.error("Tool execution error", e);
                        sseUtils.sendError(emitter, "Không thể thực hiện tác vụ. Vui lòng thử lại!");
                    }
                })
                .onCompleteResponse(r -> {
                    finalResponse.setMessage(messageBuffer.toString());
                    sseUtils.sendFinal(emitter, finalResponse);
                    emitter.complete();
                    chatbotCrudService.saveForAIAsync(request, finalResponse);
                })
                .onError(e -> {
                    handleStreamError(emitter, e);
                })
                .start();
    }

    public ChatbotResponse handleChat(ChatBotRequest request) {

        try {
            SecurityContext context = SecurityContextHolder.getContext();

            CompletableFuture<String> weatherFuture = CompletableFuture.supplyAsync(
                    () -> {
                        SecurityContextHolder.setContext(context); // Set context vào thread mới
                        try {
                            return weatherCacheService.getWeatherInfo(
                                    request.getLocation().getLatitude(),
                                    request.getLocation().getLongitude()
                            );
                        } finally {
                            SecurityContextHolder.clearContext(); // Clear sau khi xong
                        }
                    },
                    asyncExecutor
            );

            CompletableFuture<String> categoryFuture = CompletableFuture.supplyAsync(
                    () -> {
                        SecurityContextHolder.setContext(context);
                        try {
                            String weather = weatherFuture.join();
                            return categoryHintService.generateCategoryHint(
                                    request.getMessage(),
                                    request.getTime(),
                                    weather
                            );
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    },
                    asyncExecutor
            );

            String weather = weatherFuture.join();
            String categoryHint = categoryFuture.join();

            String userMessage = buildUserMessage(request, weather, categoryHint);
            ChatbotResponse response =  chatbotAIService.chat(userMessage);
            chatbotCrudService.saveForAIAsync(request, response);
            log.info("Log end");
            return response;
        }
        catch (Exception e) {
            if (isGeminiRateLimit(e)) {
                log.warn("Gemini overloaded – fallback response");
                return buildOverloadedResponse();
            }

            throw e;
        }
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
    private boolean isGeminiRateLimit(Throwable e) {
        Throwable cause = e;

        while (cause != null) {
            String msg = cause.getMessage();
            if (msg != null) {
                msg = msg.toLowerCase();
                if (
                        msg.contains("429") ||
                                msg.contains("resource_exhausted") ||
                                msg.contains("rate limit") ||
                                msg.contains("overloaded") ||
                                msg.contains("quota")
                ) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private ChatbotResponse buildOverloadedResponse() {
        return ChatbotResponse.builder()
                .data(List.of())
                .meta(Map.of(
                        "tool", "NONE",
                        "message", "Hệ thống đang bận, vui lòng thử lại sau vài giây 🙏"
                ))
                .build();
    }

    private void handleStreamError(
            SseEmitter emitter,
            Throwable e
    ) {
        try {
            if (isGeminiRateLimit(e)) {
                log.warn("Gemini rate limit / overloaded");
                sseUtils.sendError(emitter, "Hệ thống đang quá tải, vui lòng thử lại sau ít giây 🙏");
            }
            else {
                log.error("Stream error", e);
                sseUtils.sendError(emitter, "Đã xảy ra lỗi, vui lòng thử lại sau");
            }
            emitter.complete();

        } catch (Exception ex) {
            log.error("Failed to send error event", ex);
            emitter.completeWithError(ex);
        }
    }
}
