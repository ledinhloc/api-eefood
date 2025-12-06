package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.ModerationResult;
import com.eefood.recipeservice.enums.ModerationStatus;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
      String aiResponse = callGeminiAPI(prompt);
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
    Recipe recipe = recipeRepository.findById(recipeId).orElse(null);

    if (recipe != null) {
      recipe.getIngredients().size();
      recipe.getSteps().size();
    }

    return recipe;
  }






  private String callGeminiAPI(String prompt) {
    ChatRequest request = ChatRequest.builder()
      .messages(UserMessage.from(prompt))
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

    // ✅ Ingredients đã được load trong transaction
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

    // ✅ Steps đã được load trong transaction
    recipe.getSteps().stream()
      .sorted((s1, s2) -> s1.getStepNumber().compareTo(s2.getStepNumber()))
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
    prompt.append("=== MODERATION CRITERIA ===\n");
    prompt.append("Please evaluate:\n");
    prompt.append("1. Is the recipe valid and complete?\n");
    prompt.append("2. Are the ingredients appropriate and safe?\n");
    prompt.append("3. Are the cooking steps clear and logical?\n");
    prompt.append("4. Does the post content contain spam, advertising, or inappropriate content?\n");
    prompt.append("5. Is the post content relevant to the recipe?\n\n");

    prompt.append("IMPORTANT: Respond ONLY with valid JSON (no markdown):\n");
    prompt.append("{\n");
    prompt.append("  \"status\": \"APPROVED\",\n");
    prompt.append("  \"reason\": \"Brief explanation\",\n");
    prompt.append("  \"confidence\": 0.95\n");
    prompt.append("}\n");
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
      .reason("Error during moderation: " + errorMessage)
      .confidence(0.0)
      .build();
  }
}