package com.eefood.reactionservice.service.post;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.eefood.reactionservice.dto.response.*;
import com.eefood.reactionservice.enums.PostStatus;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeModerationService {

  private final GoogleAiGeminiChatModel geminiModel;

  /**
   * Đánh giá recipe có phù hợp để publish không
   */
  public ModerationResult moderateRecipe(RecipeResponse recipe) {
    String prompt = buildRecipePrompt(recipe);

    try {
      UserMessage userMessage = UserMessage.from(TextContent.from(prompt));
      ChatRequest chatRequest = ChatRequest.builder()
        .messages(userMessage)
        .build();

      ChatResponse chatResponse = geminiModel.chat(chatRequest);
      String aiResponse = chatResponse.aiMessage().text().trim();

      log.info("AI Recipe Moderation Response: {}", aiResponse);

      return parseAiResponse(aiResponse);

    } catch (Exception e) {
      log.error("AI moderation failed: {}", e.getMessage(), e);
      return ModerationResult.pending("AI processing failed");
    }
  }

  private String buildRecipePrompt(RecipeResponse recipe) {
    // Build thông tin categories
    String categories = recipe.getCategories() != null
      ? recipe.getCategories().stream()
      .map(CategoryResponse::getDescription)
      .collect(Collectors.joining(", "))
      : "N/A";

    // Build thông tin ingredients
    String ingredients = recipe.getIngredients() != null
      ? recipe.getIngredients().stream()
      .map(ri -> ri.getIngredient().getName() + " (" + ri.getQuantity() + " " + ri.getUnit() + ")")
      .collect(Collectors.joining(", "))
      : "N/A";

    // Build thông tin steps
    String steps = recipe.getSteps() != null
      ? recipe.getSteps().stream()
      .map(step -> "Bước " + step.getStepNumber() + ": " + step.getInstruction())
      .collect(Collectors.joining("\n"))
      : "N/A";

    return String.format("""
            Bạn là chuyên gia đánh giá công thức nấu ăn cho mạng xã hội ẩm thực.
            
            Phân tích công thức sau và quyết định có nên APPROVE hay REJECT:
            
            === THÔNG TIN CÔNG THỨC ===
            Tên món: %s
            Mô tả: %s
            Vùng miền: %s
            Độ khó: %s
            Thời gian chuẩn bị: %d phút
            Thời gian nấu: %d phút
            
            Danh mục: %s
            
            Nguyên liệu:
            %s
            
            Các bước thực hiện:
            %s
            
            === TIÊU CHÍ ĐÁNH GIÁ ===
            
            ✅ APPROVE NÊN:
            - Công thức rõ ràng, đầy đủ nguyên liệu và các bước
            - Nguyên liệu hợp lý, phù hợp với món ăn
            - Các bước thực hiện logic, có thể làm theo
            - Nội dung liên quan đến ẩm thực
            - Không có nội dung độc hại, spam
            
            ❌ REJECT NÊU:
            - Thiếu nguyên liệu hoặc bước quan trọng
            - Nguyên liệu không hợp lý (vd: 10kg muối cho 1 người)
            - Các bước không rõ ràng hoặc không thể thực hiện
            - Nội dung spam, quảng cáo không phù hợp
            - Có từ ngữ thô tục, không phù hợp
            - Nguyên liệu độc hại, nguy hiểm
            
            === YÊU CẦU TRẢ VỀ ===
            Trả về CHÍNH XÁC theo format JSON sau (KHÔNG có markdown, KHÔNG giải thích thêm):
            {
              "status": "APPROVED" hoặc "REJECTED",
              "reason": "Lý do ngắn gọn bằng tiếng Việt (tối đa 100 từ)",
              "confidence": 0.85
            }
            
            CHỈ TRẢ VỀ JSON, KHÔNG THÊM BẤT KỲ NỘI DUNG NÀO KHÁC.
            """,
      recipe.getTitle(),
      recipe.getDescription() != null ? recipe.getDescription() : "N/A",
      recipe.getRegion() != null ? recipe.getRegion() : "N/A",
      recipe.getDifficulty() != null ? recipe.getDifficulty().name() : "N/A",
      recipe.getPrepTime() != null ? recipe.getPrepTime() : 0,
      recipe.getCookTime() != null ? recipe.getCookTime() : 0,
      categories,
      ingredients,
      steps
    );
  }

  private ModerationResult parseAiResponse(String response) {
    try {
      // Loại bỏ markdown code block
      String cleaned = response
        .replaceAll("```json\\s*", "")
        .replaceAll("```\\s*", "")
        .trim();

      // Check status
      boolean isApproved = cleaned.contains("\"APPROVED\"");
      PostStatus status = isApproved ? PostStatus.APPROVED : PostStatus.REJECTED;

      // Extract reason
      String reason = extractJsonField(cleaned, "reason");
      if (reason == null || reason.isBlank()) {
        reason = isApproved ? "Công thức đạt yêu cầu" : "Công thức chưa đạt yêu cầu";
      }

      // Extract confidence
      double confidence = extractConfidence(cleaned);

      return new ModerationResult(status, reason, confidence);

    } catch (Exception e) {
      log.warn("Failed to parse AI response: {}", e.getMessage());
      return ModerationResult.pending("Không thể phân tích kết quả AI");
    }
  }

  private String extractJsonField(String json, String fieldName) {
    try {
      String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
      java.util.regex.Matcher m = p.matcher(json);
      return m.find() ? m.group(1) : null;
    } catch (Exception e) {
      return null;
    }
  }

  private double extractConfidence(String json) {
    try {
      String pattern = "\"confidence\"\\s*:\\s*(\\d+\\.?\\d*)";
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
      java.util.regex.Matcher m = p.matcher(json);
      return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    } catch (Exception e) {
      return 0.0;
    }
  }

  // DTO cho kết quả
  @lombok.Data
  @lombok.AllArgsConstructor
  public static class ModerationResult {
    private PostStatus status;
    private String reason;
    private Double confidence;

    public static ModerationResult pending(String reason) {
      return new ModerationResult(PostStatus.PENDING, reason, 0.0);
    }
  }
}