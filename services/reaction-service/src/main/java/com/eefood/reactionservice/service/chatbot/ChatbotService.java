package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.service.chatbot.cache.WeatherCacheService;
import com.eefood.reactionservice.util.SseUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ObjectMapper objectMapper;

    public void handleChatStream(ChatBotRequest request, SseEmitter emitter) {
        try {
            processStream(request, emitter);
        } catch (Exception e) {
            handleStreamError(emitter, e);
        }
    }

    private void processStream(ChatBotRequest request, SseEmitter emitter) {

        sseUtils.sendStatus(emitter, "Đang xử lý yêu cầu...");

        CompletableFuture<String> weatherFuture =
                CompletableFuture.supplyAsync(
                        () -> weatherCacheService.getWeatherInfo(
                                request.getLocation().getLatitude(),
                                request.getLocation().getLongitude()
                        ),
                        asyncExecutor
                );

        CompletableFuture<String> categoryFuture =
                weatherFuture.thenApplyAsync(
                        weather -> categoryHintService.generateCategoryHint(
                                request.getMessage(),
                                request.getTime(),
                                weather
                        ),
                        asyncExecutor
                );

        String weather = weatherFuture.join();
        String categoryHint = categoryFuture.join();

        sseUtils.sendStatus(emitter, "Đang phân tích yêu cầu...");

        String userMessage = buildUserMessage(request, weather, categoryHint);

        startAiStream(userMessage, request, emitter);
    }

    private void startAiStream(
            String userMessage,
            ChatBotRequest request,
            SseEmitter emitter
    ) {

        TokenStream stream = chatbotAIService.chatStream(userMessage);
        ChatbotResponse finalResponse = ChatbotResponse.empty();

        stream
                .onPartialResponse(partial -> {
                    finalResponse.setMessage(finalResponse.getMessage()+partial);
                    sseUtils.sendMessage(emitter, partial);
                })
                .onToolExecuted(tool -> {
                    ChatbotResponse toolResponse =
                            chatbotToolExecutor.execute(tool);

                    finalResponse.setData(toolResponse.getData());
                    finalResponse.setMeta(toolResponse.getMeta());
                    sseUtils.sendData(emitter, toolResponse.getData());
                })
                .onCompleteResponse(r -> {
                    sseUtils.sendFinal(emitter, finalResponse);
                    emitter.complete();
                    saveAsync(request, finalResponse);
                })
                .onError(e -> handleStreamError(emitter, e))
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
            saveAsync(request, response);
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

    private String extractTool(ChatbotResponse response) {
        if (response.getMeta() != null && response.getMeta().get("tool") != null) {
            return String.valueOf(response.getMeta().get("tool"));
        }
        return "NONE";
    }

    @Async
    @Transactional
    public void saveAsync(ChatBotRequest request, ChatbotResponse response) {
        try {
            JsonNode outputJson = objectMapper.valueToTree(response.getData());
            String tool = extractTool(response);
            createChat(request, outputJson, tool.equals("NONE") ? -1 : null, tool);
        } catch (Exception e) {
            log.error("Failed to save chat async", e);
        }
    }

    public void createChat(ChatBotRequest chatBotRequest, JsonNode output, Integer token, String tool) {
        ChatMessage chatMessage = ChatMessage.builder()
                .role(ChatRole.valueOf(chatBotRequest.getChatRole()))
                .inputImageUrl(chatBotRequest.getImageUrl())
                .inputText(chatBotRequest.getMessage())
                .chatTool(ChatTool.valueOf(tool))
                .tokenUsage(token)
                .userId(chatBotRequest.getUserId())
                .outputJson(output)
                .build();

        chatbotRepository.save(chatMessage);
    }

    // Get 2 chat history
    private String getChatHistory(Long userId) {
        List<ChatMessage> history = chatbotRepository.findTop2ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        if(history.isEmpty()) {
            return "";
        }
        return history.stream()
                .map(h -> h.getRole() + ": " + h.getInputText())
                .collect(Collectors.joining("\n"));
    }

    // Build user input
    private String buildUserMessage(ChatBotRequest request, String weather, String categoryHint) {
        return """
                CÂU HỎI NGƯỜI DÙNG:
                %s
                THÔNG TIN NGỮ CẢNH:
                - Địa điểm hiện tại: %s
                - Thời tiết hiện tại: %s
                - Thời gian: %s
                GỢI Ý DANH MỤC HỆ THỐNG:
                %s
                LỊCH SỬ HỘI THOẠI:
                %s
                HÌNH ẢNH:
                %s
                THÔNG TIN NGƯỜI DÙNG:
                %d
                """.formatted(
                request.getMessage(),
                request.getLocation().getProvince(),
                weather,
                request.getTime(),
                categoryHint,
                getChatHistory(request.getUserId()),
                request.getImageUrl(),
                request.getUserId()
                );
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
