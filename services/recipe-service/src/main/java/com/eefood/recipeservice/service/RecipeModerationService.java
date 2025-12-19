package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.ModerationResult;
import com.eefood.recipeservice.enums.ModerationStatus;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeStep;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.util.ImageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeModerationService {

  private final GoogleAiGeminiChatModel geminiChatModel;
  private final ObjectMapper objectMapper;
  private final RecipeRepository recipeRepository;

  @Transactional(readOnly = true)
  public ModerationResult moderateRecipe(Long recipeId, String postContent) {
    log.info("Starting moderation for recipeId: {}", recipeId);
    try {
      Recipe recipe = fetchRecipeWithDetails(recipeId);
      if (recipe == null) {
        return createErrorResult("Recipe not found");
      }

      String prompt = buildModerationPrompt(recipe, postContent);
//      log.info("Moderation prompt: {}", prompt);
      String aiResponse = callGeminiAPI(prompt, recipe);
      ModerationResult result = parseAIResponse(aiResponse);

      log.info("Moderation completed for recipe: {} - Status: {}",
        recipe.getTitle(), result.getStatus());

      return result;
    } catch (Exception e) {
      log.error("Error during recipe moderation: {}", e.getMessage(), e);
      return createErrorResult(e.getMessage());
    }
  }

  private Recipe fetchRecipeWithDetails(Long recipeId) {
    Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(recipeId).orElse(null);

    if (recipe != null) {
      recipe.getIngredients().size();
      recipe.getSteps().size();
    }

    return recipe;
  }

  private String  callGeminiAPI(String textPrompt, Recipe recipe) {
    List<Content> contents = new ArrayList<>();
    //Thêm text prompt
    contents.add(TextContent.from(textPrompt));

    // Thêm hình ảnh chính của recipe (nếu có)
    if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
      String base64Image = ImageUtils.downloadAndEncodeImage(recipe.getImageUrl());
      if (base64Image != null) {
        String mimeType = ImageUtils.getMimeType(recipe.getImageUrl());
        Image image = Image.builder()
          .base64Data(base64Image)
          .mimeType(mimeType)
          .build();
        contents.add(ImageContent.from(image));
        log.info("Added main recipe image to analysis");
      }
    }

    // Thêm hình ảnh từ các steps (tối đa 5 ảnh để tránh quá tải)
    int imageCount = 0;
    int maxImages = 5;

    for (RecipeStep step : recipe.getSteps()) {
      if (imageCount >= maxImages) break;

      if (step.getImageUrls() != null && !step.getImageUrls().isEmpty()) {
        for (String imageUrl : step.getImageUrls()) {
          if (imageCount >= maxImages) break;

          String base64Image = ImageUtils.downloadAndEncodeImage(imageUrl);
          if (base64Image != null) {
            String mimeType = ImageUtils.getMimeType(imageUrl);
            Image image = Image.builder()
              .base64Data(base64Image)
              .mimeType(mimeType)
              .build();
            contents.add(ImageContent.from(image));
            imageCount++;
            log.info("Added step {} image to analysis", step.getStepNumber());
          }
        }
      }
    }

    ChatRequest request = ChatRequest.builder()
      .messages(dev.langchain4j.data.message.UserMessage.from(contents))
      .build();

    ChatResponse response = geminiChatModel.chat(request);
    return response.aiMessage().text();
  }

  private String buildModerationPrompt(Recipe recipe, String postContent) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are a food recipe content moderator. ");
    prompt.append("Analyze the following recipe and post content.\n\n");

    appendRecipeInformation(prompt, recipe);
    appendIngredients(prompt, recipe);
    appendCookingSteps(prompt, recipe);
    appendPostContent(prompt, postContent);
    appendModerationCriteria(prompt);

    return prompt.toString();
  }

  private void appendRecipeInformation(StringBuilder prompt, Recipe recipe) {
    prompt.append("=== RECIPE INFORMATION ===\n");
    prompt.append("Title: ").append(recipe.getTitle()).append("\n");
    prompt.append("Description: ").append(recipe.getDescription()).append("\n");
    prompt.append("Region: ").append(recipe.getRegion()).append("\n");
    prompt.append("Difficulty: ").append(recipe.getDifficulty()).append("\n");
    prompt.append("Prep Time: ").append(recipe.getPrepTime()).append(" minutes\n");
    prompt.append("Cook Time: ").append(recipe.getCookTime()).append(" minutes\n\n");
  }

  private void appendIngredients(StringBuilder prompt, Recipe recipe) {
    prompt.append("=== INGREDIENTS ===\n");

    recipe.getIngredients().forEach(recipeIngredient -> {
      prompt.append("- ")
        .append(recipeIngredient.getIngredient().getName())
        .append(": ")
        .append(recipeIngredient.getQuantity())
        .append(" ")
        .append(recipeIngredient.getUnit())
        .append("\n");
    });

    prompt.append("\n");
  }

  private void appendCookingSteps(StringBuilder prompt, Recipe recipe) {
    prompt.append("=== COOKING STEPS ===\n");

    recipe.getSteps().stream()
      .sorted(Comparator.comparing(RecipeStep::getStepNumber))
      .forEach(step -> {
        prompt.append(step.getStepNumber())
          .append(". ")
          .append(step.getInstruction())
          .append("\n");
      });

    prompt.append("\n");
  }

  private void appendPostContent(StringBuilder prompt, String postContent) {
    if (postContent != null && !postContent.trim().isEmpty()) {
      prompt.append("=== USER POST CONTENT ===\n");
      prompt.append(postContent).append("\n\n");
    }
  }

  private void appendModerationCriteria(StringBuilder prompt) {
    prompt.append("\n=== YÊU CẦU ĐÁNH GIÁ ===\n");
    prompt.append("Bạn là chuyên gia kiểm duyệt công thức nấu ăn. ");
    prompt.append("Đánh giá theo 6 tiêu chí sau (mỗi tiêu chí 0-10 điểm):\n\n");

    prompt.append("1. RECIPE_COMPLETENESS (0-10): Công thức có đầy đủ không?\n");
    prompt.append("   - Đủ thông tin nguyên liệu, số lượng\n");
    prompt.append("   - Đủ các bước thực hiện\n");
//    prompt.append("   - Thời gian hợp lý\n\n");

    prompt.append("2. INGREDIENT_SAFETY (0-10): Nguyên liệu có an toàn không?\n");
    prompt.append("   - Không có nguyên liệu độc hại\n");
    prompt.append("   - Không có thành phần gây dị ứng nghiêm trọng\n");
    prompt.append("   - Nguyên liệu phổ biến, dễ kiếm\n\n");

    prompt.append("3. STEP_CLARITY (0-10): Các bước có rõ ràng không?\n");
    prompt.append("   - Trình bày theo thứ tự logic\n");
    prompt.append("   - Hướng dẫn chi tiết, dễ hiểu\n");
    prompt.append("   - Không bỏ sót bước quan trọng\n\n");

    prompt.append("4. CONTENT_APPROPRIATE (0-10): Nội dung có phù hợp không?\n");
    prompt.append("   - Không chứa spam, quảng cáo\n");
    prompt.append("   - Không có nội dung tục tĩu\n");
    prompt.append("   - Không có thông tin sai lệch về sức khỏe\n\n");

    prompt.append("5. CONTENT_RELEVANCE (0-10): Nội dung có liên quan không?\n");
    prompt.append("   - Mô tả đúng về món ăn\n");
    prompt.append("   - Không đi chệch hướng\n");
    prompt.append("   - Bổ sung thông tin hữu ích\n\n");

    prompt.append("6. MEDIA_QUALITY (0-10):  Chất lượng hình ảnh/video\n");
    prompt.append("   - Hình ảnh rõ nét, không mờ\n");
    prompt.append("   - Món ăn trông hấp dẫn, bắt mắt\n");
    prompt.append("   - Hình ảnh/video phù hợp với từng bước\n");
    prompt.append("   - Không có watermark spam hoặc logo quá lớn\n");
    prompt.append("   - Video (nếu có) ổn định, âm thanh rõ ràng\n");
    prompt.append("   - LƯU Ý: Nếu KHÔNG có hình ảnh/video -> cho 5 điểm (trung bình)\n");
    prompt.append("   - Nếu CÓ hình ảnh/video chất lượng tốt -> 8-10 điểm\n");
    prompt.append("   - Nếu CÓ nhưng chất lượng kém -> 3-5 điểm\n\n");

    prompt.append("=== QUY TẮC CHẤM ĐIỂM ===\n");
    prompt.append("- Mỗi tiêu chí: 0-10 điểm\n");
    prompt.append("- Tổng điểm = (tổng 6 tiêu chí / 60) × 100 = 0-100 điểm\n");
    prompt.append("- APPROVED: >= 60 điểm\n");
//    prompt.append("- PENDING: 50-69 điểm\n");
    prompt.append("- REJECTED: < 60 điểm\n\n");

    prompt.append("=== ĐỊNH DẠNG TRẢ LỜI (JSON) ===\n");
    prompt.append("Trả về ĐÚNG format JSON sau (không thêm markdown, không thêm text):\n");
    prompt.append("{\n");
    prompt.append("  \"status\": \"APPROVED\",\n");
    prompt.append("  \"summary\": \"Tóm tắt đánh giá tổng quan bằng tiếng Việt (2-3 câu)\",\n");
    prompt.append("  \"totalScore\": 85.0,\n");
    prompt.append("  \"recipeCompleteness\": 9,\n");
    prompt.append("  \"completenessNote\": \"Công thức đầy đủ với nguyên liệu và bước làm chi tiết\",\n");
    prompt.append("  \"ingredientSafety\": 10,\n");
    prompt.append("  \"safetyNote\": \"Tất cả nguyên liệu an toàn và phổ biến\",\n");
    prompt.append("  \"stepClarity\": 8,\n");
    prompt.append("  \"clarityNote\": \"Các bước rõ ràng, có thể thêm chi tiết về nhiệt độ\",\n");
    prompt.append("  \"contentAppropriate\": 9,\n");
    prompt.append("  \"appropriatenessNote\": \"Nội dung phù hợp, không có spam\",\n");
    prompt.append("  \"contentRelevance\": 9,\n");
    prompt.append("  \"relevanceNote\": \"Nội dung liên quan trực tiếp đến công thức\",\n");
    prompt.append("  \"mediaQuality\": 9,\n");
    prompt.append("  \"mediaQualityNote\": \"Hình ảnh rõ nét, món ăn trông hấp dẫn, video hướng dẫn chi tiết\"\n");
    prompt.append("}\n\n");

    prompt.append(" LƯU Ý QUAN TRỌNG:\n");
    prompt.append("- Tất cả ghi chú phải bằng TIẾNG VIỆT\n");
    prompt.append("- Không thêm ```json``` hoặc markdown\n");
    prompt.append("- Chỉ trả về JSON thuần túy\n");
    prompt.append("- totalScore = ((recipeCompleteness + ingredientSafety + stepClarity + contentAppropriate + contentRelevance + mediaQuality) / 60) × 100\n");
  }

  private ModerationResult parseAIResponse(String aiResponse) {
    try {
      String cleanJson = extractJson(aiResponse);
      return objectMapper.readValue(cleanJson, ModerationResult.class);

    } catch (JsonProcessingException e) {
      log.error("Failed to parse AI JSON response: {}", e.getMessage());
      log.error("AI response content: {}", aiResponse);
      return createErrorResult("AI returned invalid JSON format");
    }
  }

  private String extractJson(String aiResponse) {
    if (aiResponse == null || aiResponse.trim().isEmpty()) {
      throw new IllegalArgumentException("AI response is empty");
    }

    String cleaned = aiResponse
      .replaceAll("```json\\s*", "")
      .replaceAll("```\\s*", "")
      .trim();

    int startIndex = cleaned.indexOf("{");
    int endIndex = cleaned.lastIndexOf("}");

    if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
      throw new IllegalArgumentException("No valid JSON found in AI response");
    }

    return cleaned.substring(startIndex, endIndex + 1);
  }

  private ModerationResult createErrorResult(String errorMessage) {
    return ModerationResult.builder()
      .status(ModerationStatus.PENDING)
      .summary("Error during moderation: " + errorMessage)
      .build();
  }
}