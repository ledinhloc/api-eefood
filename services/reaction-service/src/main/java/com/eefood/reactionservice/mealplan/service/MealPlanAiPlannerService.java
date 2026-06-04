package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealItem;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanGenerateRequest;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealPlanAiPlannerService {

    private final GoogleAiGeminiChatModel geminiChatModel;
    private final ObjectMapper objectMapper;

    public List<GeneratedMealItem> generatePlan(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            MealPlanGenerateRequest request,
            List<MealPlanAiCandidate> candidates,
            int days
    ) {
        try {
            String prompt = buildGeneratePrompt(user, bodyMetrics, request, candidates, days);
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(UserMessage.from(prompt))
                    .build();

            ChatResponse chatResponse = geminiChatModel.chat(chatRequest);
            return parseGeneratedItems(chatResponse.aiMessage().text(), request.getStartDate(), candidates);
        } catch (Exception e) {
            log.warn("Meal plan AI generation failed: {}", e.getMessage());
            return List.of();
        }
    }

    // Gom dữ liệu user và candidate recipe thành prompt ngắn gọn, có cấu trúc.
    private String buildGeneratePrompt(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            MealPlanGenerateRequest request,
            List<MealPlanAiCandidate> candidates,
            int days
    ) {
        String candidateJson;
        try {
            candidateJson = objectMapper.writeValueAsString(
                    candidates.stream()
                            .map(this::toPromptCandidate)
                            .toList()
            );
        } catch (Exception e) {
            candidateJson = "[]";
        }

        return """
                Bạn là một chuyên gia dinh dưỡng, giúp tôi tạo một meal plan ban đầu và chỉ được trả về JSON hợp lệ.
                Quy tắc:
                - Chỉ trả về JSON hợp lệ, không được dùng markdown, không giải thích thêm.
                - Chỉ được sử dụng recipeId có trong candidate_recipes.
                - Tạo đúng 3 bữa mỗi ngày: BREAKFAST, LUNCH, DINNER.
                - Tổng số item phải bằng days * 3.
                - planDate phải bắt đầu từ start_date và kéo dài đúng số ngày được cung cấp.
                - Phải chú ý dị ứng thực phẩm một cách nghiêm ngặt.
                - Nếu healthConditions có tiểu đường/diabetes thì ưu tiên món ít đường, nhiều chất xơ.
                - Nếu healthConditions có cao huyết áp/hypertension thì ưu tiên món sodium thấp.
                - Nếu healthConditions có mỡ máu/cholesterol/tim mạch thì hạn chế món nhiều fat.
                - Nếu healthConditions có dạ dày/gastric thì hạn chế món cay, chua.
                Cấu trúc JSON cần trả về:
                {
                  "items": [
                    {
                      "planDate": "yyyy-MM-dd",
                      "mealSlot": "BREAKFAST|LUNCH|DINNER",
                      "itemOrder": 1,
                      "recipeId": 123,
                      "servings": 1,
                      "note": "ghi chú ngắn bằng tiếng Việt"
                    }
                  ]
                }
                user_profile: %s
                goal: %s
                start_date: %s
                days: %d
                candidate_recipes: %s
                """.formatted(
                buildUserProfileSummary(user, bodyMetrics, request.getGoal()),
                request.getGoal(),
                request.getStartDate(),
                days,
                candidateJson
        );
    }

    // Chỉ nhận các item JSON hợp lệ và phải trỏ tới candidate recipe đã biết.
    private Map<String, Object> toPromptCandidate(MealPlanAiCandidate candidate) {
        return Map.of(
                "recipeId", candidate.getRecipeId(),
                "title", candidate.getTitle(),
                "ingredients", candidate.getIngredientKeywords(),
                "nutrition", toPromptNutrition(candidate)
        );
    }

    private Map<String, Object> toPromptNutrition(MealPlanAiCandidate candidate) {
        return Map.of(
                "calories", defaultDouble(candidate.getNutrition().getTotalCalories()),
                "protein", defaultDouble(candidate.getNutrition().getTotalProtein()),
                "carbs", defaultDouble(candidate.getNutrition().getTotalCarb()),
                "fat", defaultDouble(candidate.getNutrition().getTotalFat()),
                "fiber", defaultDouble(candidate.getNutrition().getTotalFiber()),
                "sugar", defaultDouble(candidate.getNutrition().getTotalSugar()),
                "sodium", defaultDouble(candidate.getNutrition().getTotalSodium())
        );
    }

    private List<GeneratedMealItem> parseGeneratedItems(
            String raw,
            LocalDate startDate,
            List<MealPlanAiCandidate> candidates
    ) throws Exception {
        String normalized = stripMarkdownCodeFence(raw);
        JsonNode root = objectMapper.readTree(normalized);
        JsonNode itemsNode = root.path("items");
        if (!itemsNode.isArray()) {
            return List.of();
        }

        Set<Long> candidateIds = candidates.stream()
                .map(MealPlanAiCandidate::getRecipeId)
                .collect(Collectors.toSet());

        List<GeneratedMealItem> result = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            Long recipeId = itemNode.path("recipeId").isNumber() ? itemNode.path("recipeId").asLong() : null;
            String mealSlotRaw = itemNode.path("mealSlot").asText(null);
            String planDateRaw = itemNode.path("planDate").asText(null);
            if (recipeId == null || mealSlotRaw == null || planDateRaw == null || !candidateIds.contains(recipeId)) {
                continue;
            }

            MealPlanAiCandidate candidate = candidates.stream()
                    .filter(c -> Objects.equals(c.getRecipeId(), recipeId))
                    .findFirst()
                    .orElse(null);
            if (candidate == null) {
                continue;
            }

            result.add(GeneratedMealItem.builder()
                    .planDate(LocalDate.parse(planDateRaw))
                    .mealSlot(MealSlot.valueOf(mealSlotRaw.toUpperCase(Locale.ROOT)))
                    .itemOrder(itemNode.path("itemOrder").asInt(1))
                    .servings(itemNode.path("servings").asInt(1))
                    .note(itemNode.path("note").asText(null))
                    .candidate(candidate)
                    .build());
        }

        return result.stream()
                .filter(item -> !item.getPlanDate().isBefore(startDate))
                .toList();
    }

    // Tóm tắt user profile thành các field cần thiết để AI lên kế hoạch.
    private String buildUserProfileSummary(UserResponse user, UserBodyMetricsResponse bodyMetrics, String goal) {
        if (user == null) {
            return "{}";
        }

        int age = user.getDob() != null ? (int) ChronoUnit.YEARS.between(user.getDob(), LocalDate.now()) : -1;

        return """
                {
                  "gender": "%s",
                  "age": %d,
                  "heightCm": "%s",
                  "weightKg": "%s",
                  "activityLevel": "%s",
                  "allergies": %s,
                  "eatingPreferences": %s,
                  "dietaryPreferences": %s,
                  "healthConditions": %s
                }
                """.formatted(
                user.getGender(),
                age,
                bodyMetrics != null ? bodyMetrics.getHeightCm() : null,
                bodyMetrics != null ? bodyMetrics.getWeightKg() : null,
                user.getActivityLevel(),
                normalizeList(user.getAllergies()),
                normalizeList(user.getEatingPreferences()),
                normalizeList(user.getDietaryPreferences()),
                normalizeList(user.getHealthConditions())
        );
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    // Gemini có thể bọc JSON trong markdown fence nên cần bỏ trước khi parse.
    private String stripMarkdownCodeFence(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "");
            text = text.replaceFirst("```$", "").trim();
        }
        return text;
    }

    private double defaultDouble(Double value) {
        return value == null ? 0d : value;
    }
}
