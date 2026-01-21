package com.eefood.reactionservice.service.chatbot;

import com.eefood.reactionservice.dto.SearchResult;
import com.eefood.reactionservice.dto.request.ChatBotRequest;
import com.eefood.reactionservice.dto.request.LocationInfoRequest;
import com.eefood.reactionservice.dto.response.PostResponse;
import com.eefood.reactionservice.dto.response.ResponseData;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotResponse;
import com.eefood.reactionservice.dto.response.chatbot.ChatbotSearchCriteria;
import com.eefood.reactionservice.enums.ChatRole;
import com.eefood.reactionservice.enums.ChatTool;
import com.eefood.reactionservice.model.chatbot.ChatMessage;
import com.eefood.reactionservice.repository.chatbot.ChatbotRepository;
import com.eefood.reactionservice.repository.httpclient.IamClient;
import com.eefood.reactionservice.service.follow.FollowService;
import com.eefood.reactionservice.service.post.PostScrollSearchService;
import com.eefood.reactionservice.service.post.PostSearchService;
import com.eefood.reactionservice.util.ImageUtils;
import com.eefood.reactionservice.util.PromptLoader;
import com.eefood.reactionservice.util.SecurityUtil;
import com.eefood.reactionservice.util.WeatherCodeMapperUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    private final ChatbotRepository chatbotRepository;
    private final GoogleAiGeminiChatModel geminiChatModel;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;
    private final IamClient iamClient;
    private final FollowService followService;
    private final PostScrollSearchService postScrollSearchService;
    private final ChromaRagService chromaRagService;
    private final ChromaEmbeddingService chromaEmbeddingService;

    public ChatbotResponse handleChat(ChatBotRequest request) {
        // Load text từ file chatbot_system_prmopt.txt và thay các tham số vào
        String systemPrompt = buildSystemPrompt(request);

        log.info("=== CHATBOT REQUEST ===");
        log.info("User message: {}", request.getMessage());
        log.info("Image URL: {}", request.getImageUrl());
        log.info("Location: {}", request.getLocation().getProvince());
        log.info("Role: {}", request.getChatRole());
        log.info("Tool: {}", request.getChatTool());

        // Đưa lên Gemini để xử lý phân loại nghiệp vụ
        String llmResponse = callGemini(systemPrompt, request.getImageUrl());
        log.info("Raw Gemini criteria response: {}", llmResponse);

        String jsonClean = extractJson(llmResponse);
        log.info("Cleaned Gemini JSON: {}", jsonClean);


        // Chuyển kết quả thành ChatbotSearchCriteria
        ChatbotSearchCriteria criteria;
        try {
            criteria = objectMapper.readValue(jsonClean, ChatbotSearchCriteria.class);
            log.info("=== EXTRACTED CRITERIA ===");
            log.info("Keyword: {}", criteria.getKeyword());
            log.info("Location: {}", criteria.getLocation());
            log.info("Difficulty: {}", criteria.getDifficulty());
            log.info("Category: {}", criteria.getCategory());
            log.info("MaxCookTime: {}", criteria.getMaxCookTime());
        } catch (Exception e) {
            log.error("Failed to parse Gemini criteria", e);
            return ChatbotResponse.builder()
                    .message("Mình chưa hiểu rõ yêu cầu của bạn 😢. Hãy thực hiện lại!!!")
                    .data(List.of())
                    .build();
        }

        log.info("Extracted criteria: {}", criteria);

        // Đưa từ khóa cho els tìm ra tập dữ liệu mẫu
        Long currentUserId = request.getUserId();
        UserResponse user = null;
        List<Long> newFollowings = List.of();
        List<Long> oldFollowings = List.of();

        try {
            log.info("Logged-in user: {}", currentUserId);

            if(currentUserId != null){
                // Lấy thông tin user và followings CHỈ KHI đã login
                ResponseData<UserResponse> userResponse = iamClient.getUserById(currentUserId);
                user = userResponse.getData();
                newFollowings = followService.getNewFollowings(currentUserId);
                oldFollowings = followService.getOldFollowings(currentUserId);
            }
        } catch (Exception e) {
            // Guest user - không có token
            log.info("Guest user - no personalization applied");
        }

        //Lấy danh sách postIds từ Elasticsearch
        List<Long> candidatePostIds = postScrollSearchService.searchAllPostIds(
                criteria.getKeyword(),
                criteria.getLocation(),
                criteria.getDifficulty(),
                criteria.getCategory(),
                criteria.getMaxCookTime(),
                user,
                newFollowings,
                oldFollowings
        );

        if (candidatePostIds.isEmpty()) {
            return ChatbotResponse.builder()
                    .message("Xin lỗi, mình không thể thực hiện được việc trên 😢")
                    .data(List.of())
                    .build();
        }
        // Đảm bảo chỉ embed những post trong candidate list
        chromaEmbeddingService.ensureEmbeddingsExist(candidatePostIds);

        // Dùng vector db chroma để lấy ra top 5 danh sách phù hợp nhất
        List<PostResponse> topPosts   = chromaRagService.retrieveTopKSimilarPosts(candidatePostIds,request.getMessage(),criteria.getIngredient(),5);
        log.info("=== CHROMA RAG RESULT ===");
        log.info("Top similar posts: {}", topPosts.size());

        if (topPosts.isEmpty()) {
            return ChatbotResponse.builder()
                    .message("Mình đã tìm được một số món, nhưng chưa chọn được món phù hợp nhất 😅")
                    .data(List.of())
                    .build();
        }

        // Chuyển 5 bài post sang json để đưa lên gemini lần 2 tạo kết quả
        String postJson = convertPostToJsonObject(topPosts);

        // Đọc text từ file chatbot_response_prompt.txt và đưa lên gemini
        String template = promptLoader.load("prompts/chatbot_response_prompt.txt");
        String finalPrompt = buildFinalPrompt(
                request.getMessage(),
                topPosts,
                criteria
        );
        String llmFinalResponse = callGemini(finalPrompt, null);

        return buildFinalResponse(request, llmFinalResponse, criteria.getTool());
    }

    private String buildSystemPrompt(ChatBotRequest request) {
        return promptLoader.load("prompts/chatbot_system_prompt.txt")
                .replace("{chat_history}", Optional.ofNullable(getChatHistory(request.getUserId())).orElse(""))
                .replace("{user_message}", Optional.ofNullable(request.getMessage()).orElse(""))
                .replace("{weather}",
                        Optional.ofNullable(request.getLocation())
                                .map(loc -> WeatherCodeMapperUtils
                                        .getCurrentWeather(loc.getLatitude(), loc.getLongitude())
                                        .getDescription())
                                .orElse(""))
                .replace("{image_url}", Optional.ofNullable(request.getImageUrl()).orElse(""))
                .replace("{location}",
                        Optional.ofNullable(request.getLocation().getProvince())
                                .orElse(""))
                .replace("{time_present}", Optional.ofNullable(request.getTime()).orElse(""));
    }

    private String buildFinalPrompt(
            String userQuery,
            List<PostResponse> posts,
            ChatbotSearchCriteria criteria
    ) {
        // Convert posts to JSON
        String postJson = convertPostToJsonObject(posts);

        // Build criteria description
        String criteriaDescription = buildCriteriaDescription(criteria);

        // Load template và replace placeholders
        String template = promptLoader.load("prompts/chatbot_response_prompt.txt");

        return template
                .replace("{user_query}", userQuery)
                .replace("{criteria_description}", criteriaDescription)
                .replace("{post_data}", postJson);
    }

    private String buildCriteriaDescription(ChatbotSearchCriteria criteria) {
        List<String> filters = new ArrayList<>();

        if (criteria.getMaxCookTime() != null) {
            filters.add("thời gian nấu tối đa " + criteria.getMaxCookTime() + " phút");
        }

        if (criteria.getDifficulty() != null && !criteria.getDifficulty().isEmpty()) {
            String difficultyText = switch (criteria.getDifficulty().toUpperCase()) {
                case "EASY" -> "dễ làm";
                case "MEDIUM" -> "độ khó trung bình";
                case "HARD" -> "nâng cao";
                default -> criteria.getDifficulty();
            };
            filters.add("độ khó: " + difficultyText);
        }

        if (criteria.getCategory() != null && !criteria.getCategory().isEmpty()) {
            filters.add("danh mục: " + criteria.getCategory());
        }

        if (criteria.getLocation() != null && !criteria.getLocation().isEmpty()) {
            filters.add("khu vực: " + criteria.getLocation());
        }

        if (criteria.getIngredient() != null && !criteria.getIngredient().isEmpty()) {
            filters.add("sử dụng nguyên liệu: " + String.join(", ", criteria.getIngredient()));
        }

        if (filters.isEmpty()) {
            return "phù hợp với yêu cầu của bạn";
        }

        return String.join(", ", filters);
    }

    private ChatbotResponse buildFinalResponse(ChatBotRequest request, String llmResponse, String tool) {
        try {
            String cleaned = extractJson(llmResponse);
            JsonNode json = objectMapper.readTree(cleaned);

            createChat(
                    request,
                    json,
                    json.has("token") ? json.get("token").asInt() : null,
                    tool
            );

            return ChatbotResponse.builder()
                    .message(json.get("message").asText())
                    .data(objectMapper.convertValue(json.get("data"), List.class))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse final Gemini response", e);
            return ChatbotResponse.builder()
                    .message("Đây là các món ăn phù hợp với bạn 🍽️")
                    .data(List.of())
                    .build();
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

    public String callGemini(String prompt, String imageUrl) {
        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(prompt));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            contents.add(handleImageUrl(imageUrl));
        }

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(UserMessage.from(contents))
                .build();

        ChatResponse response = geminiChatModel.chat(chatRequest);
        return response.aiMessage().text();
    }

    private String getChatHistory(Long userId) {
        List<ChatMessage> history = chatbotRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        return history.stream()
                .map(h -> h.getRole() + ": " + h.getInputText())
                .collect(Collectors.joining("\n"));
    }

    private String convertPostToJsonObject(List<PostResponse> posts) {
        try {
            Map<String, Object> wrapper = Map.of(
                    "total", posts == null ? 0 : posts.size(),
                    "items", posts == null ? List.of() : posts
            );

            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            log.error("Error converting posts to JSON", e);
            return "{\"total\":0,\"items\":[]}";
        }
    }

    private ImageContent handleImageUrl(String imageUrl) {
        String base64Image = ImageUtils.downloadAndEncodeImage(imageUrl);
        if (base64Image != null) {
            String mimeType = ImageUtils.getMimeType(imageUrl);
            Image image = Image.builder()
                    .base64Data(base64Image)
                    .mimeType(mimeType)
                    .build();
            return  ImageContent.from(image);
        }
        return null;
    }

    private String extractJson(String llmResponse) {
        if (llmResponse == null) return null;

        // Xóa ```json ... ```
        String cleaned = llmResponse
                .replaceAll("(?s)^```json", "")   // bỏ ```json ở đầu
                .replaceAll("```$", "")          // bỏ ``` ở cuối
                .trim();

        // Nếu Gemini trả về text trước JSON, lấy phần từ { đầu tiên
        int firstBrace = cleaned.indexOf("{");
        int lastBrace = cleaned.lastIndexOf("}");

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned;
    }
}
