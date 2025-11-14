package com.eefood.recipeservice.service;

import com.eefood.common.avro.RecipeEvent;
import com.eefood.recipeservice.dto.request.RecipeIngredientRequest;
import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.request.RecipeStepRequest;
import com.eefood.recipeservice.dto.response.*;
import com.eefood.recipeservice.enums.Difficulty;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.kafka.RecipeProducer;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.*;
import com.eefood.recipeservice.repository.CategoryRepository;
import com.eefood.recipeservice.repository.IngredientRepository;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.repository.RecipeStepRepository;
import com.eefood.recipeservice.repository.httpclient.IamClient;
import com.eefood.recipeservice.util.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
  private final RecipeRepository recipeRepository;
  private final RecipeStepRepository stepRepository;
  private final CategoryRepository categoryRepository;
  private final RecipeMapper recipeMapper;
  private final IngredientRepository ingredientRepository;
  private final IamClient iamClient;
  private final RecipeIndexer recipeIndexer;
  private final RecipeProducer recipeProducer;
  private final SecurityUtil securityUtil;
  private final GoogleAiGeminiChatModel gemini;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Lấy nội dung HTML bằng Jsoup */
  private String fetchWebContent(String url) {
    try {
      return Jsoup.connect(url)
        .userAgent("Mozilla/5.0")
        .timeout(10000)
        .get()
        .html(); // dùng .html() nếu cần đầy đủ HTML
    } catch (Exception e) {
      throw new RuntimeException("Fetch thất bại: " + e.getMessage());
    }
  }

  public RecipeResponse extractAndCreate(String url) {
    // FETCH HTML CONTENT
    String html = fetchWebContent(url);

    // PROMPT AI
    String prompt = """
You are an advanced Recipe Extraction Engine.

Your task:
- Read and analyze the HTML content provided below.
- Extract ONLY the relevant recipe information.
- Ignore all unrelated content such as ads, banners, user comments, tracking scripts, stylesheets.

STRICT OUTPUT RULES (MUST FOLLOW EXACTLY):
1. Output MUST be **pure JSON only**.
2. Do NOT include any explanation, description, or natural language.
3. Do NOT wrap JSON in code blocks (` ```json` or ```).
4. JSON MUST start with '{' and end with '}'.
5. JSON MUST match the EXACT schema below.
6. All string fields MUST be plain strings. No special formatting.
7. difficulty MUST be one of: "EASY", "MEDIUM", "HARD".
8. ingredients[] MUST contain objects { "name", "quantity", "unit" }
9. steps[] MUST contain objects { "stepNumber", "instruction" }
10. If a field is missing from HTML, return a reasonable empty value: 
   - "" for strings
   - 0 for numbers
   - [] for arrays
11. Do NOT add comments inside JSON.

YOUR OUTPUT JSON SCHEMA (MUST MATCH EXACTLY):
{
  "title": "",
  "description": "",
  "region": "",
  "imageUrl": "",
  "videoUrl": "",
  "categories": [],
  "prepTime": 0,
  "cookTime": 0,
  "difficulty": "EASY",
  "ingredients": [
    { "name": "", "quantity": 0, "unit": "" }
  ],
  "steps": [
    { "stepNumber": 1, "instruction": "", "imageUrl": "","videoUrl": "", "stepTime": 0 }
  ]
}

NOW ANALYZE THE FOLLOWING HTML AND RETURN ONLY JSON:

===== HTML CONTENT START =====
%s
===== HTML CONTENT END =====
""".formatted(html);


    log.info("------------"+ prompt);

    // Gửi prompt đến Gemini
    ChatRequest request = ChatRequest.builder()
      .messages(UserMessage.from(prompt))
      .build();
    ChatResponse response = gemini.chat(request);
    String aiJson = response.aiMessage().text();


    log.info("------------"+ aiJson);
    // PARSE JSON → DTO
    RecipeExtractDTO dto;
    try{
      dto = objectMapper.readValue(aiJson, RecipeExtractDTO.class);
    }catch (JsonProcessingException e){
        throw new RuntimeException("AI trả JSON sai format: " + e.getMessage());
    }

    /** tạo mới Category
     * neu cate ton tai -> lay id
     * neu chua ton tai -> tao roi lay id
     * */
    List<Long> categoryIds = dto.getCategories().stream()
      .map(c -> categoryRepository.findByDescriptionIgnoreCase(c)
        .orElseGet(() -> {
          Category newC = new Category();
          newC.setDescription(c);
          return categoryRepository.save(newC);
        }).getId()
      ).toList();

    //tạo mới Ingredient
    List<RecipeIngredientRequest> ingredientRequests =
      dto.getIngredients().stream().map(i -> {

        Ingredient ingredient = ingredientRepository.findByNameIgnoreCase(i.getName())
          .orElseGet(() -> {
            Ingredient newIng = new Ingredient();
            newIng.setName(i.getName());
            return ingredientRepository.save(newIng);
          });

        return RecipeIngredientRequest.builder()
          .ingredientId(ingredient.getId())
          .name(i.getName())
          .quantity(i.getQuantity())
          .unit(i.getUnit())
          .build();
      }).toList();

    // Map sang RecipeRequest
    RecipeRequest req = RecipeRequest.builder()
      .title(dto.getTitle())
      .description(dto.getDescription())
      .region(dto.getRegion())
      .imageUrl(dto.getImageUrl())
      .videoUrl(dto.getVideoUrl())
      .prepTime(dto.getPrepTime())
      .cookTime(dto.getCookTime())
      .difficulty(Difficulty.valueOf(dto.getDifficulty().toUpperCase()))
      .categoryIds(categoryIds)
      .ingredients(ingredientRequests)
      .steps(dto.getSteps())
      .build();

    Long authorId = securityUtil.getCurrentUserId();
    // SAVE RECIPE
    return createRecipe(req, authorId);
  }

  public Recipe getEntityRecipe(Long id) {
    return recipeRepository.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
  }

  public void deleteRecipeById(Long id) {
    Recipe recipe =
        recipeRepository
            .findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

    recipe.setIsDeleted(true);
    //xoa step
    recipe.getSteps().forEach(step -> step.setIsDeleted(true));
    //xoa ingredient
    recipe.getIngredients().forEach(ingredient -> ingredient.setIsDeleted(true));
    recipeRepository.save(recipe);
    //delete els
    recipeIndexer.deleteRecipe(id);
  }

  @Transactional(readOnly = true)
  public Page<RecipeResponse> searchRecipes(
    String title,
    String description,
    String region,
    Difficulty difficulty,
    Long categoryId,
    Long authorId,
    Pageable pageable
  ) {
    Specification<Recipe> spec = Specification.allOf(
      RecipeSpecification.isNotDeleted(),
      RecipeSpecification.hasTitle(title),
      RecipeSpecification.hasDescription(description),
      RecipeSpecification.hasRegion(region),
      RecipeSpecification.hasDifficulty(difficulty),
      RecipeSpecification.hasCategoryId(categoryId),
      RecipeSpecification.hasAuthor(authorId)
    );
    return recipeRepository.findAll(spec, pageable).map(recipeMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public RecipeResponse getRecipeById(Long id) {
    Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
    return recipeMapper.toResponse(recipe);
  }

  @Transactional(readOnly = true)
  public RecipeSummaryResponse getRecipeSummaryById(Long id) {
    Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
    return recipeMapper.toSummaryResponse(recipe);
  }

  public RecipeDetailResponse getRecipeDetail(Long id) {
    Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
    RecipeDetailResponse recipeResponse = recipeMapper.toDetailResponse(recipe);

    // --- Lấy thông tin user ---
    UserInfo userInfo = iamClient.getUserInfo(recipe.getAuthorId()).getData();
    recipeResponse.setUserId(userInfo.getId());
    recipeResponse.setUsername(userInfo.getUsername());
    recipeResponse.setEmail(userInfo.getEmail());
    recipeResponse.setAvatarUrl(userInfo.getAvatarUrl());

    // --- Sắp xếp steps theo stepNumber ---
    if (recipeResponse.getSteps() != null) {
      recipeResponse.setSteps(
        recipeResponse.getSteps().stream()
          .sorted(Comparator.comparingInt(StepResponse::getStepNumber))
          .toList()
      );
    }

    // --- Sắp xếp ingredients theo id ---
    if (recipeResponse.getIngredients() != null) {
      recipeResponse.setIngredients(
        recipeResponse.getIngredients().stream()
          .sorted(Comparator.comparingLong(RecipeIngredientResponse::getId))
          .toList()
      );
    }

    return recipeResponse;
  }

  @Transactional
  public RecipeResponse createRecipe(RecipeRequest request, Long currentUser) {
    Recipe recipe = recipeMapper.toEntity(request);

    // set categories
    List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
    recipe.setCategories(new HashSet<>(categories));

    // set ingredients
    if(request.getIngredients() != null) {
      for (RecipeIngredientRequest ingredientReq : request.getIngredients()){
        RecipeIngredient recipeIngredient = recipeMapper.toEntity(ingredientReq);
        Ingredient ingredient = ingredientRepository.findById(ingredientReq.getIngredientId())
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + ingredientReq.getIngredientId()));
        recipeIngredient.setIngredient(ingredient);
        recipe.addIngredient(recipeIngredient);
      }
    }

    // add steps
    if (request.getSteps() != null) {
      for (RecipeStepRequest stepReq : request.getSteps()) {
        RecipeStep step = recipeMapper.toEntity(stepReq);
        recipe.addStep(step);
      }
    }
    recipe.setAuthorId(currentUser);
    Recipe saved = recipeRepository.save(recipe);
    //luu els
//    recipeIndexer.saveOrUpdateRecipe(saved);
    return recipeMapper.toResponse(saved);
  }

  @Transactional
  public RecipeResponse updateRecipe(Long id, RecipeRequest request) {
    Recipe recipe = recipeRepository.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    recipe.setTitle(request.getTitle());
    recipe.setDescription(request.getDescription());
    recipe.setRegion(request.getRegion());
    recipe.setImageUrl(request.getImageUrl());
    recipe.setVideoUrl(request.getVideoUrl());
    recipe.setPrepTime(request.getPrepTime());
    recipe.setCookTime(request.getCookTime());
    recipe.setDifficulty(request.getDifficulty());

    // update categories
    List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
    recipe.setCategories(new HashSet<>(categories));
    /* ========== UPDATE INGREDIENTS ========== */
    List<Long> requestIngredientIds = request.getIngredients().stream()
            .map(RecipeIngredientRequest::getId)
            .filter(Objects::nonNull)
            .toList();

    // lấy danh sách ingredients hiện có trong recipe
    Set<RecipeIngredient> existingIngredients = recipe.getIngredients();

    // soft delete ingredient không có trong request
    for (RecipeIngredient ri : existingIngredients) {
      if (ri.getId() != null && !requestIngredientIds.contains(ri.getId())) {
        ri.setIsDeleted(true);
      }
    }

    // update or create ingredients
    for (RecipeIngredientRequest ingReq : request.getIngredients()) {
      if (ingReq.getId() == null) {
        // thêm mới
        RecipeIngredient newIng = recipeMapper.toEntity(ingReq);
        Ingredient ingredient = ingredientRepository.findById(ingReq.getIngredientId())
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + ingReq.getIngredientId()));
        newIng.setIngredient(ingredient);
        newIng.setRecipe(recipe);
        recipe.addIngredient(newIng);
      } else {
        // update
        RecipeIngredient ri = existingIngredients.stream()
                .filter(e -> e.getId().equals(ingReq.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("RecipeIngredient not found"));

        if (Boolean.TRUE.equals(ri.getIsDeleted())) {
          ri.setIsDeleted(false);
        }

        ri.setQuantity(ingReq.getQuantity());
        ri.setUnit(ingReq.getUnit());

        // nếu ingredientId thay đổi → update reference
        if (!ri.getIngredient().getId().equals(ingReq.getIngredientId())) {
          Ingredient ingredient = ingredientRepository.findById(ingReq.getIngredientId())
                  .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + ingReq.getIngredientId()));
          ri.setIngredient(ingredient);
        }
      }
    }

    // update steps
    List<Long> requestStepIds = request.getSteps().stream()
      .map(RecipeStepRequest::getId)
      .filter(Objects::nonNull)
      .toList();

    List<RecipeStep> existingSteps = stepRepository.findByRecipeIdAndIsDeletedFalse(id);

    // soft delete missing steps
    for (RecipeStep step : existingSteps) {
      if (!requestStepIds.contains(step.getId())) {
        step.setIsDeleted(true);
        stepRepository.save(step);
      }
    }

    // update or create steps
    for (RecipeStepRequest stepReq : request.getSteps()) {
      if (stepReq.getId() == null) {
        RecipeStep newStep = recipeMapper.toEntity(stepReq);
        newStep.setRecipe(recipe);
        stepRepository.save(newStep);
      } else {
        RecipeStep step = stepRepository.findById(stepReq.getId())
          .orElseThrow(() -> new EntityNotFoundException("Step not found"));

        // khôi phục nếu trước đó bị xóa mềm
        if (Boolean.TRUE.equals(step.getIsDeleted())) {
          step.setIsDeleted(false);
        }

        step.setStepNumber(stepReq.getStepNumber());
        step.setInstruction(stepReq.getInstruction());
        step.setImageUrl(stepReq.getImageUrl());
        step.setVideoUrl(stepReq.getVideoUrl());
        step.setStepTime(stepReq.getStepTime());
        stepRepository.save(step);
      }
    }

    // Gửi event kafka sau khi update
    RecipeEvent event = RecipeEvent.newBuilder()
      .setId(recipe.getId())
      .setTitle(recipe.getTitle())
      .setDescription(recipe.getDescription())
      .setRegion(recipe.getRegion())
      .setImageUrl(recipe.getImageUrl())
      .setPrepTime(recipe.getPrepTime())
      .setCookTime(recipe.getCookTime())
      .setDifficulty(recipe.getDifficulty() != null ? recipe.getDifficulty().name() : null)
      .setCategories(
        recipe.getCategories().stream()
          .map(Category::getDescription)
          .collect(Collectors.toList())
      )
      .setIngredientKeywords(
        recipe.getIngredients().stream()
          .map(ri -> ri.getIngredient().getName())
          .collect(Collectors.toList())
      )
      .build();
    recipeProducer.sendRecipe(event);
    //els
//    recipeIndexer.saveOrUpdateRecipe(recipe);
    return recipeMapper.toResponse(recipe);
  }
}