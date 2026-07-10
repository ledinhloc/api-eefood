package com.eefood.reactionservice.mealplan.service;

import com.eefood.reactionservice.dto.response.UserBodyMetricsResponse;
import com.eefood.reactionservice.dto.response.UserHeightResponse;
import com.eefood.reactionservice.dto.response.UserResponse;
import com.eefood.reactionservice.dto.response.UserWeightResponse;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealItem;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealPlanResult;
import com.eefood.reactionservice.mealplan.dto.ai.GeneratedMealReplacement;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.request.MealPlanGenerateRequest;
import com.eefood.reactionservice.mealplan.enums.MealSlot;
import com.eefood.reactionservice.mealplan.model.MealPlanItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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

    private final OpenAiChatModel openAiModel;
    private final ObjectMapper objectMapper;

    public GeneratedMealPlanResult generateInitialMealPlan(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            MealPlanGenerateRequest request,
            List<MealPlanAiCandidate> candidates,
            int days
    ) {
        return requestMealPlanFromAi(user, bodyMetrics, request, candidates, days, null, null, Set.of(), null);
    }

    public GeneratedMealPlanResult generateMealPlanContinuation(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            MealPlanGenerateRequest request,
            List<MealPlanAiCandidate> candidates,
            int days,
            List<UserWeightResponse> weightHistory,
            List<UserHeightResponse> heightHistory,
            Set<Long> recentRecipeIds,
            String currentMealPlanNote
    ) {
        return requestMealPlanFromAi(
                user,
                bodyMetrics,
                request,
                candidates,
                days,
                weightHistory,
                heightHistory,
                recentRecipeIds,
                currentMealPlanNote
        );
    }

    private GeneratedMealPlanResult requestMealPlanFromAi(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            MealPlanGenerateRequest request,
            List<MealPlanAiCandidate> candidates,
            int days,
            List<UserWeightResponse> weightHistory,
            List<UserHeightResponse> heightHistory,
            Set<Long> recentRecipeIds,
            String currentMealPlanNote
    ) {
        try {
            String prompt = buildMealPlanPrompt(
                    user,
                    bodyMetrics,
                    request,
                    candidates,
                    days,
                    weightHistory,
                    heightHistory,
                    recentRecipeIds,
                    currentMealPlanNote
            );
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(UserMessage.from(prompt))
                    .build();

            long startTime = System.currentTimeMillis();
            ChatResponse chatResponse = openAiModel.chat(chatRequest);
            log.info("OpenAI meal plan generation completed in {} ms", System.currentTimeMillis() - startTime);
            return parseGeneratedMealPlanResult(chatResponse.aiMessage().text(), request.getStartDate(), candidates);
        } catch (Exception e) {
            log.warn("Meal plan AI generation failed: {}", e.getMessage());
            return GeneratedMealPlanResult.builder()
                    .items(List.of())
                    .build();
        }
    }

    public List<GeneratedMealReplacement> generateMealReplacements(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            String goal,
            String reason,
            List<MealPlanItem> replacedItems,
            List<MealPlanAiCandidate> candidates
    ) {
        try {
            String prompt = buildMealReplacementPrompt(user, bodyMetrics, goal, reason, replacedItems, candidates);
            long startTime = System.currentTimeMillis();
            ChatResponse chatResponse = openAiModel.chat(ChatRequest.builder()
                    .messages(UserMessage.from(prompt))
                    .build());
            log.info("OpenAI meal plan replacement completed in {} ms", System.currentTimeMillis() - startTime);
            return parseGeneratedMealReplacements(chatResponse.aiMessage().text(), replacedItems, candidates);
        } catch (Exception e) {
            log.warn("Meal plan AI replacement failed: {}", e.getMessage());
            return List.of();
        }
    }

    // Gom dữ liệu user và candidate recipe thành prompt ngắn gọn, có cấu trúc.
    private String buildMealPlanPrompt(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            MealPlanGenerateRequest request,
            List<MealPlanAiCandidate> candidates,
            int days,
            List<UserWeightResponse> weightHistory,
            List<UserHeightResponse> heightHistory,
            Set<Long> recentRecipeIds,
            String currentMealPlanNote
    ) {
        String candidateJson;
        String recentRecipeJson;
        try {
            candidateJson = objectMapper.writeValueAsString(
                    candidates.stream()
                            .map(this::toCandidatePromptData)
                            .toList()
            );
        } catch (Exception e) {
            candidateJson = "[]";
        }
        try {
            recentRecipeJson = objectMapper.writeValueAsString(
                    (recentRecipeIds == null ? Set.<Long>of() : recentRecipeIds).stream()
                            .sorted(Comparator.naturalOrder())
                            .toList()
            );
        } catch (Exception e) {
            recentRecipeJson = "[]";
        }

        String bodyHistorySection = "";
        String planInstruction = "Bạn là một chuyên gia dinh dưỡng, giúp tôi tạo một meal plan ban đầu và chỉ được trả về JSON hợp lệ.";
        if (weightHistory != null && heightHistory != null) {
            planInstruction = "Bạn là một chuyên gia dinh dưỡng, giúp tôi tạo phần tiếp theo của meal plan và chỉ được trả về JSON hợp lệ.";
            try {
                String bodyHistoryJson = objectMapper.writeValueAsString(Map.of(
                        "weightHistory", weightHistory.stream()
                                .map(item -> Map.of(
                                        "date", item.getRecordedDate(),
                                        "weightKg", item.getWeightKg()
                                ))
                                .toList(),
                        "heightHistory", heightHistory.stream()
                                .map(item -> Map.of(
                                        "date", item.getRecordedDate(),
                                        "heightCm", item.getHeightCm()
                                ))
                                .toList()
                ));
                bodyHistorySection = """
                        - Đây là kế hoạch tiếp tục. Hãy đánh giá toàn bộ quá trình thay đổi cân nặng và chiều cao từ ngày bắt đầu meal plan.
                        - Điều chỉnh lựa chọn món theo xu hướng thực tế và goal; không phản ứng quá mức với một lần đo đơn lẻ.
                        body_progress_history: %s
                        """.formatted(bodyHistoryJson);
            } catch (Exception e) {
                bodyHistorySection = """
                        - Đây là kế hoạch tiếp tục nhưng không thể đọc lịch sử cơ thể; hãy dùng thông tin hiện tại.
                        body_progress_history: {"weightHistory":[],"heightHistory":[]}
                        """;
            }
        }

        return """
                %s
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
                  "mealPlanNote": "ghi chú tổng quan ngắn cho kế hoạch",
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
                current_meal_plan_note: %s
                recent_recipe_ids: %s
                meal_plan_rules: [
                  "Han che chon recipeId trong recent_recipe_ids neu van con lua chon khac phu hop.",
                  "Khong dung cung mot recipeId qua 1 lan trong cung mot ngay.",
                  "Han che lap recipeId trong toan bo ke hoach neu candidate_recipes du so mon.",
                  "BREAKFAST nen nhe hon LUNCH va DINNER; LUNCH va DINNER co the nhieu calories/protein hon.",
                  "BREAKFAST nen uu tien mon nhanh/de, hop thoi quen an sang Viet Nam nhu bun, pho, hu tieu, banh mi; han che mon khong binh thuong cho buoi sang nhu oc hoac mon nhau.",
                  "Uu tien mon phu hop region/thoi quen an uong cua user neu khong mau thuan voi di ung, suc khoe va goal.",
                  "note phai giai thich ngan vi sao mon phu hop voi goal/suc khoe/bua an do."
                ]
                candidate_recipes: %s
                %s
                """.formatted(
                planInstruction,
                buildUserProfileSummary(user, bodyMetrics, request.getGoal()),
                request.getGoal(),
                request.getStartDate(),
                days,
                currentMealPlanNote == null ? "" : currentMealPlanNote,
                recentRecipeJson,
                candidateJson,
                bodyHistorySection
        );
    }

    // Chỉ nhận các item JSON hợp lệ và phải trỏ tới candidate recipe đã biết.
    private Map<String, Object> toCandidatePromptData(MealPlanAiCandidate candidate) {
        return Map.ofEntries(
                Map.entry("recipeId", candidate.getRecipeId()),
                Map.entry("title", defaultString(candidate.getTitle())),
                Map.entry("region", defaultString(candidate.getRegion())),
                Map.entry("difficulty", defaultString(candidate.getDifficulty())),
                Map.entry("ingredients", candidate.getIngredientKeywords() == null ? List.of() : candidate.getIngredientKeywords()),
                Map.entry("nutrition", toNutritionPromptData(candidate))
        );
    }

    private Map<String, Object> toNutritionPromptData(MealPlanAiCandidate candidate) {
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

    private String buildMealReplacementPrompt(
            UserResponse user,
            UserBodyMetricsResponse bodyMetrics,
            String goal,
            String reason,
            List<MealPlanItem> replacedItems,
            List<MealPlanAiCandidate> candidates
    ) throws Exception {
        String replacedItemsJson = objectMapper.writeValueAsString(replacedItems.stream()
                .map(item -> Map.ofEntries(
                        Map.entry("mealPlanItemId", item.getId()),
                        Map.entry("planDate", item.getPlanDate().toString()),
                        Map.entry("mealSlot", item.getMealSlot().name()),
                        Map.entry("recipeId", item.getRecipeId() == null ? 0L : item.getRecipeId()),
                        Map.entry("recipeTitle", item.getRecipeTitle() == null ? "" : item.getRecipeTitle()),
                        Map.entry("servings", item.getPlannedServings() == null ? 1 : item.getPlannedServings()),
                        Map.entry("calories", item.getCalories() == null ? 0 : item.getCalories()),
                        Map.entry("protein", item.getProtein() == null ? 0 : item.getProtein()),
                        Map.entry("carbs", item.getCarbs() == null ? 0 : item.getCarbs()),
                        Map.entry("fat", item.getFat() == null ? 0 : item.getFat()),
                        Map.entry("sugar", item.getSugar() == null ? 0 : item.getSugar()),
                        Map.entry("sodium", item.getSodium() == null ? 0 : item.getSodium())
                ))
                .toList());
        String candidateJson = objectMapper.writeValueAsString(candidates.stream()
                .map(this::toCandidatePromptData)
                .toList());

        return """
                Bạn là chuyên gia dinh dưỡng. Chọn món thay thế theo các quy tắc:
                - Dùng đúng mỗi mealPlanItemId trong replaced_items một lần.
                - Chỉ dùng recipeId trong candidate_recipes và không trùng món.
                - Ưu tiên lý do người dùng, sau đó đến dị ứng, sức khỏe, mục tiêu và dinh dưỡng gần món cũ.
                - Đánh giá món theo ngữ cảnh ẩm thực thông thường.
                - note phải nêu rõ đặc điểm đáp ứng yêu cầu.
                - Chỉ trả JSON hợp lệ, không markdown hoặc giải thích.
                Cấu trúc:
                {
                  "replacements": [
                    {
                      "mealPlanItemId": 101,
                      "recipeId": 205,
                      "servings": 1,
                      "note": "Lý do chọn món ngắn gọn bằng tiếng Việt"
                    }
                  ]
                }
                user_profile: %s
                goal: %s
                user_reason: %s
                replaced_items: %s
                candidate_recipes: %s
                """.formatted(
                buildUserProfileSummary(user, bodyMetrics, goal),
                goal,
                reason == null ? "" : reason.trim(),
                replacedItemsJson,
                candidateJson
        );
    }

    private GeneratedMealPlanResult parseGeneratedMealPlanResult(
            String raw,
            LocalDate startDate,
            List<MealPlanAiCandidate> candidates
    ) throws Exception {
        String normalized = stripMarkdownCodeFence(raw);
        JsonNode root = objectMapper.readTree(normalized);
        String mealPlanNote = root.path("mealPlanNote").asText(null);
        JsonNode itemsNode = root.path("items");
        if (!itemsNode.isArray()) {
            return GeneratedMealPlanResult.builder()
                    .mealPlanNote(normalizeBlank(mealPlanNote))
                    .items(List.of())
                    .build();
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

        return GeneratedMealPlanResult.builder()
                .mealPlanNote(normalizeBlank(mealPlanNote))
                .items(result.stream()
                        .filter(item -> !item.getPlanDate().isBefore(startDate))
                        .toList())
                .build();
    }

    private List<GeneratedMealReplacement> parseGeneratedMealReplacements(
            String raw,
            List<MealPlanItem> replacedItems,
            List<MealPlanAiCandidate> candidates
    ) throws Exception {
        JsonNode replacementsNode = objectMapper.readTree(stripMarkdownCodeFence(raw)).path("replacements");
        if (!replacementsNode.isArray()) {
            return List.of();
        }

        Set<Long> itemIds = replacedItems.stream()
                .map(MealPlanItem::getId)
                .collect(Collectors.toSet());
        Map<Long, MealPlanAiCandidate> candidatesByRecipeId = candidates.stream()
                .collect(Collectors.toMap(MealPlanAiCandidate::getRecipeId, candidate -> candidate));
        List<GeneratedMealReplacement> result = new ArrayList<>();

        for (JsonNode replacementNode : replacementsNode) {
            Long itemId = replacementNode.path("mealPlanItemId").isNumber()
                    ? replacementNode.path("mealPlanItemId").asLong()
                    : null;
            Long recipeId = replacementNode.path("recipeId").isNumber()
                    ? replacementNode.path("recipeId").asLong()
                    : null;
            if (itemId == null || recipeId == null || !itemIds.contains(itemId)
                    || !candidatesByRecipeId.containsKey(recipeId)) {
                continue;
            }

            result.add(GeneratedMealReplacement.builder()
                    .mealPlanItemId(itemId)
                    .servings(Math.max(1, replacementNode.path("servings").asInt(1)))
                    .note(replacementNode.path("note").asText(null))
                    .candidate(candidatesByRecipeId.get(recipeId))
                    .build());
        }
        return result;
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

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
