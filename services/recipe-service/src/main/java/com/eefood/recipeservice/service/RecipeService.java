package com.eefood.recipeservice.service;

import com.eefood.common.avro.RecipeEvent;
import com.eefood.recipeservice.dto.request.*;
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
import com.eefood.recipeservice.repository.httpclient.ReactionClient;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
  private final AlternateIngredientService alternateIngredientService;

  private final ReactionClient reactionClient;
  private static final int MIN_USER_ID = 1;
  private static final int MAX_USER_ID = 20;
  private static final Random random = new Random();

  public RecipeCompareResponse compare(RecipeCompareRequest compareRequest) {
    if(compareRequest.getRecipeIdA().equals(compareRequest.getRecipeIdB())) {
      throw ExceptionUtil.badRequest(ErrorMessage.RECIPE_NOT_SAME);
    }

    Recipe recipeA = recipeRepository
            .findByIdAndIsDeletedFalse(compareRequest.getRecipeIdA())
            .orElseThrow(()-> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

    Recipe recipeB = recipeRepository
            .findByIdAndIsDeletedFalse(compareRequest.getRecipeIdB())
            .orElseThrow(()-> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

    return RecipeCompareResponse.builder()
            .recipeA(recipeMapper.toCompareResponse(recipeA))
            .recipeB(recipeMapper.toCompareResponse(recipeB))
            .build();
  }

  //tim theo ten neu chua co thi tao
  private Set<Category> resolveCategories(List<String> categoryNames) {
    if(categoryNames == null || categoryNames.isEmpty()) {
      return new HashSet<>();
    }

    return categoryNames.stream()
      .map(categoryName -> categoryRepository.findByDescriptionIgnoreCase(categoryName)
        .orElseGet(() ->{
          Category newCategory = new Category();
          newCategory.setDescription(categoryName);
          Category saved = categoryRepository.save(newCategory);
          log.info("Saved category: {}", saved.getDescription());
          return saved;
        })).collect(Collectors.toSet());
  }

  private List<RecipeIngredient> resolveIngredients(List<RecipeIngredientRequest> ingredientDtos) {
    if(ingredientDtos == null || ingredientDtos.isEmpty()) {
      return new ArrayList<>();
    }

    return ingredientDtos.stream()
      .map(ingredientDto ->{
        Ingredient ingredient = ingredientRepository.findByNameIgnoreCase(ingredientDto.getName())
          .orElseGet(() -> {
            Ingredient newIngredient = new Ingredient();
            newIngredient.setName(ingredientDto.getName());
            Ingredient saved = ingredientRepository.save(newIngredient);
            log.info("Saved ingredient: {}", saved.getDescription());
            return saved;
          });

        RecipeIngredient recipeIngredient = RecipeIngredient.builder()
          .ingredient(ingredient)
          .quantity(ingredientDto.getQuantity())
          .unit(ingredientDto.getUnit())
          .build();

        return recipeIngredient;
      }).toList();
  }

  public RecipeResponse saveExtractResultWithPost(RecipeExtractDTO dto) {

    long randomUserId = MIN_USER_ID + random.nextLong(MAX_USER_ID - MIN_USER_ID + 1);
    //Lưu recipe
    RecipeResponse recipeResponse = saveExtractResult(dto, randomUserId);
    createPostAsync(recipeResponse.getId(), randomUserId, dto.getTitle());
    return recipeResponse;
  }

  //  Tạo post bất đồng bộ
  private void createPostAsync(Long recipeId, Long userId, String recipeTitle) {
    SecurityContext context = SecurityContextHolder.getContext();

    CompletableFuture.runAsync(() -> {
      try {
        // Set SecurityContext vào thread mới
        SecurityContextHolder.setContext(context);
        createPostForUser(recipeId, userId, recipeTitle);
      } finally {
        SecurityContextHolder.clearContext();
      }
    });
  }

  private void createPostForUser(Long recipeId, Long userId, String recipeTitle) {
    try {
      PostCreateRequest postRequest = PostCreateRequest.builder()
        .recipeId(recipeId)
        .content("Cùng thử nấu " + recipeTitle )
        .build();

      // Gọi Post Service qua Feign Client với userId
      reactionClient.createPost(postRequest, userId);

      log.info("----Created post for recipe: {} user: {}", recipeId, userId);
    } catch (Exception e) {
      log.error("----Failed to create post for recipe: {} user: {}: {}",
        recipeId, userId, e.getMessage());
    }
  }

  public RecipeResponse saveExtractResult(RecipeExtractDTO dto, Long userId) {
    // Convert IngredientExtractDTO sang RecipeIngredientRequest
    List<RecipeIngredientRequest> ingredientRequests = dto.getIngredients().stream()
      .map(i -> RecipeIngredientRequest.builder()
        .name(i.getName())
        .quantity(i.getQuantity())
        .unit(i.getUnit())
        .build()
      )
      .toList();

    // Map sang RecipeRequest
    RecipeRequest request = RecipeRequest.builder()
      .title(dto.getTitle())
      .description(dto.getDescription())
      .region(dto.getRegion())
      .imageUrl(dto.getImageUrl())
      .videoUrl(dto.getVideoUrl())
      .prepTime(dto.getPrepTime())
      .cookTime(dto.getCookTime())
      .difficulty(Difficulty.valueOf(dto.getDifficulty().toUpperCase()))
      .categories(dto.getCategories())
      .ingredients(ingredientRequests)
      .steps(dto.getSteps())
      .build();

    return createRecipe(request, userId);
  }

  private static final Set<String> ALLOWED_TAGS = Set.of(
    "p","div","span","img","video","source","iframe",
    "ul","ol","li","h1","h2","h3","h4",
    "strong","b","i","em","u","br","a",
    "section","article","header","main","figure",
    "table","tbody","thead","tr","td","th"
  );

  private String cleanHtml(String html) {
    Document doc = Jsoup.parse(html);

    // Remove junk
    doc.select("script, style, svg, noscript, meta, link").remove();

    // Sanitize media tags
    doc.select("img, video, source, iframe").forEach(tag -> {
      String src = tag.attr("abs:src");
      tag.clearAttributes();
      if (!src.isEmpty()) tag.attr("src", src);
    });

    // WHITELIST — safe unwrap
    List<Element> elements = new ArrayList<>(doc.body().select("*"));

    for (Element el : elements) {
      String tag = el.tagName();

      // Skip root nodes
      if (el.parent() == null || tag.equals("body") || tag.equals("html"))
        continue;

      if (!ALLOWED_TAGS.contains(tag)) {
        el.unwrap(); // Remove tag but KEEP TEXT
      }
    }

    return doc.body().html();
  }

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
//    String html = fetchWebContent(url);
    String rawHtml = fetchWebContent(url);
    String html = cleanHtml(rawHtml);

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
3. JSON MUST match the EXACT schema below.
4. All string fields MUST be plain strings. No special formatting.
5. difficulty MUST be one of: "EASY", "MEDIUM", "HARD".
6. ingredients[] MUST contain objects { "name", "quantity", "unit" } where quantity is a number (can be decimal)
7. steps[] MUST contain objects { "stepNumber", "instruction", "imageUrls", "videoUrls", "stepTime" }
8. If a field is missing from HTML, return a reasonable empty value:
   - "" for strings
   - 0 for numbers
   - [] for arrays
9. Do NOT add comments inside JSON.
10. Do NOT mix ingredients into categories.
11. Ingredients MUST be separated into individual items — never group many ingredients into one string.
12. Ingredients MUST NOT include prefix like "Gia vị", "Nguyên liệu", "Mẹo", etc.
13. Categories MUST be cooking categories (e.g., "Món Việt", "Món gà", "Món kho"), NOT ingredients.
14. If time is not found, set to 0.
15. steps[] MUST be separated correctly with actual instructions.
ALWAYS RETURN ONLY PURE JSON. DO NOT USE MARKDOWN, BACKTICKS, OR ANY TEXT OUTSIDE JSON.


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
    { "name": "", "quantity": 0.0, "unit": "" }
  ],
  "steps": [
    { "stepNumber": 1, "instruction": "", "imageUrls": [], "videoUrls": [], "stepTime": 0 }
  ]
}

NOW ANALYZE THE FOLLOWING HTML AND RETURN ONLY JSON:

===== HTML CONTENT START =====
%s
===== HTML CONTENT END =====
""".formatted(html);

//    log.info("------------"+ prompt);

    // Gửi prompt đến Gemini
    ChatRequest request = ChatRequest.builder()
      .messages(UserMessage.from(prompt))
      .build();
    ChatResponse response = gemini.chat(request);
    String aiJson = extractJson(response.aiMessage().text());

//    log.info("------------ AiJson: "+ aiJson);
    // PARSE JSON → DTO
    RecipeExtractDTO dto;
    try{
      dto = objectMapper.readValue(aiJson, RecipeExtractDTO.class);
    }catch (JsonProcessingException e){
        throw new RuntimeException("AI trả JSON sai format: " + e.getMessage());
    }
    Long userId = securityUtil.getCurrentUserId();

    return saveExtractResult(dto, userId);
  }

  public Recipe getEntityRecipe(Long id) {
    return recipeRepository.findByIdAndIsDeletedFalse(id)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));
  }

  private String extractJson(String aiRaw) {
    if (aiRaw == null || aiRaw.isEmpty()) return "{}";

    // Tìm JSON bắt đầu với { và kết thúc với }
    int firstBrace = aiRaw.indexOf("{");
    int lastBrace = aiRaw.lastIndexOf("}");
    if (firstBrace >= 0 && lastBrace > firstBrace) {
      return aiRaw.substring(firstBrace, lastBrace + 1).trim();
    }

    // fallback
    return aiRaw.trim();
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
  public Page<RecipeResponse> searchDraftRecipes(
          String title,
          String description,
          String region,
          Difficulty difficulty,
          Long categoryId,
          Long authorId,
          Pageable pageable
  ) {
    ResponseData<List<PostPublishResponse>> data = reactionClient.getPostsPublishByUser();
    List<PostPublishResponse> publishedPosts = data.getData();

    Set<Long> publishedRecipeIds = publishedPosts.stream()
            .map(PostPublishResponse::getRecipeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Specification<Recipe> spec = Specification.allOf(
            RecipeSpecification.isNotDeleted(),
            RecipeSpecification.hasTitle(title),
            RecipeSpecification.hasDescription(description),
            RecipeSpecification.hasRegion(region),
            RecipeSpecification.hasDifficulty(difficulty),
            RecipeSpecification.hasCategoryId(categoryId),
            RecipeSpecification.hasAuthor(authorId)
    );

    Page<Recipe> page = recipeRepository.findAll(spec, pageable);

    List<RecipeResponse> draftRecipes = page.stream()
            .filter(recipe -> !publishedRecipeIds.contains(recipe.getId()))
            .map(recipeMapper::toResponse)
            .collect(Collectors.toList());

    return new PageImpl<>(draftRecipes, pageable, page.getTotalElements());
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

    List<RecipeIngredientResponse> riResponse = recipeResponse.getIngredients().stream().toList();

    List<IngredientAlterResponse> ingreAltResponse = alternateIngredientService.getIngredientAndSub(id);

    Map<Long, IngredientAlterResponse> alterMap = ingreAltResponse.stream()
            .collect(Collectors.toMap(
                    ia -> ia.getIngredient().getId(),
                    ia -> ia
            ));

    List<RecipeIngredientResponse> updatedIngredients = riResponse.stream()
            .map(ri -> {
              IngredientDetailResponse current = ri.getIngredient();

              IngredientAlterResponse alter = alterMap.get(current.getId());

              IngredientDetailResponse result;

              if (alter != null && alter.getSelectedSubstitute() != null) {
                IngredientResponse sub = alter.getSelectedSubstitute();

                result = IngredientDetailResponse.builder()
                        .id(sub.getId())
                        .name(sub.getName())
                        .description(sub.getDescription())
                        .image(sub.getImage())
                        .originalId(current.getId())
                        .build();
              } else {
                result = IngredientDetailResponse.builder()
                        .id(current.getId())
                        .name(current.getName())
                        .description(current.getDescription())
                        .image(current.getImage())
                        .originalId(current.getId())
                        .build();
              }

              ri.setIngredient(result);
              return ri;
            })
            .toList();

    recipeResponse.setIngredients(updatedIngredients);

    return recipeResponse;
  }

  @Transactional
  public RecipeResponse createRecipe(RecipeRequest request, Long currentUser) {
    Recipe recipe = recipeMapper.toEntity(request);
    // Xử lý categories (tìm hoặc tạo mới)
    Set<Category> categories = resolveCategories(request.getCategories());
    recipe.setCategories(categories);

    // Xử lý ingredients (tìm hoặc tạo mới)
    List<RecipeIngredient> recipeIngredients = resolveIngredients(request.getIngredients());
    recipeIngredients.forEach(recipe::addIngredient);

    // add steps
    if (request.getSteps() != null) {
      for (RecipeStepRequest stepReq : request.getSteps()) {
        RecipeStep step = recipeMapper.toEntity(stepReq);
        recipe.addStep(step);
      }
    }

    recipe.setAuthorId(currentUser);
    Recipe saved = recipeRepository.save(recipe);
    log.info("Created recipe: {} by user: {}", saved.getId(), currentUser);
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
    Set<Category> categories = resolveCategories(request.getCategories());
    recipe.setCategories(categories);

    /* ========== UPDATE INGREDIENTS ========== */
    Set<RecipeIngredient> existingIngredients = recipe.getIngredients();

//  Build map: ingredient_name → RecipeIngredient
    Map<String, RecipeIngredient> existingMap = existingIngredients.stream()
      .filter(ri -> !ri.getIsDeleted())
      .collect(Collectors.toMap(
        ri -> ri.getIngredient().getName().toLowerCase(),
        ri -> ri
      ));

//  Track which ingredients được giữ lại
    Set<String> processedNames = new HashSet<>();

//  Loop qua request: update nếu có, create nếu chưa
    for (RecipeIngredientRequest ingReq : request.getIngredients()) {
      String ingredientName = ingReq.getName().toLowerCase();
      processedNames.add(ingredientName);

      // Tìm hoặc tạo Ingredient
      Ingredient ingredient = ingredientRepository.findByNameIgnoreCase(ingReq.getName())
        .orElseGet(() -> {
          Ingredient newIng = new Ingredient();
          newIng.setName(ingReq.getName());
          return ingredientRepository.save(newIng);
        });

      if (existingMap.containsKey(ingredientName)) {
        // UPDATE existing
        RecipeIngredient existing = existingMap.get(ingredientName);
        existing.setIngredient(ingredient);
        existing.setQuantity(ingReq.getQuantity());
        existing.setUnit(ingReq.getUnit());
      } else {
        //  CREATE new
        RecipeIngredient newRi = RecipeIngredient.builder()
          .ingredient(ingredient)
          .quantity(ingReq.getQuantity())
          .unit(ingReq.getUnit())
          .recipe(recipe)
          .build();
        recipe.addIngredient(newRi);
      }
    }

// Soft delete ingredients không còn trong request
    existingIngredients.stream()
      .filter(ri -> !processedNames.contains(ri.getIngredient().getName().toLowerCase()))
      .forEach(ri -> ri.setIsDeleted(true));

//    List<Long> requestIngredientIds = request.getIngredients().stream()
//            .map(RecipeIngredientRequest::getId)
//            .filter(Objects::nonNull)
//            .toList();
//
//    // lấy danh sách ingredients hiện có trong recipe
//    Set<RecipeIngredient> existingIngredients = recipe.getIngredients();
//
//    // soft delete ingredient không có trong request
//    for (RecipeIngredient ri : existingIngredients) {
//      if (ri.getId() != null && !requestIngredientIds.contains(ri.getId())) {
//        ri.setIsDeleted(true);
//      }
//    }
//
//    // update or create ingredients
//    for (RecipeIngredientRequest ingReq : request.getIngredients()) {
//      if (ingReq.getId() == null) {
//        // thêm mới
//        RecipeIngredient newIng = recipeMapper.toEntity(ingReq);
//        Ingredient ingredient = ingredientRepository.findById(ingReq.getIngredientId())
//                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + ingReq.getIngredientId()));
//        newIng.setIngredient(ingredient);
//        newIng.setRecipe(recipe);
//        recipe.addIngredient(newIng);
//      } else {
//        // update
//        RecipeIngredient ri = existingIngredients.stream()
//                .filter(e -> e.getId().equals(ingReq.getId()))
//                .findFirst()
//                .orElseThrow(() -> new EntityNotFoundException("RecipeIngredient not found"));
//
//        if (Boolean.TRUE.equals(ri.getIsDeleted())) {
//          ri.setIsDeleted(false);
//        }
//
//        ri.setQuantity(ingReq.getQuantity());
//        ri.setUnit(ingReq.getUnit());
//
//        // nếu ingredientId thay đổi → update reference
//        if (!ri.getIngredient().getId().equals(ingReq.getIngredientId())) {
//          Ingredient ingredient = ingredientRepository.findById(ingReq.getIngredientId())
//                  .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + ingReq.getIngredientId()));
//          ri.setIngredient(ingredient);
//        }
//      }
//    }

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
        step.setImageUrls(stepReq.getImageUrls());
        step.setVideoUrls(stepReq.getVideoUrls());
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