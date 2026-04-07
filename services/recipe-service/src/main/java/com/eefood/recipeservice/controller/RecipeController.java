package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.RecipeExtractDTO;
import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.request.RequestUrl;
import com.eefood.recipeservice.dto.response.*;
import com.eefood.recipeservice.enums.Difficulty;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.enums.SuccessMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.service.AlternateIngredientService;
import com.eefood.recipeservice.service.RecipeSearchService;
import com.eefood.recipeservice.service.RecipeService;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {
  private final SecurityUtil securityUtil;

  private final RecipeService recipeService;
  private final RecipeSearchService recipeSearchService;
  private final AlternateIngredientService alternateIngredientService;

  @GetMapping("/{id}/ingredient-substitutes")
  public ResponseData<List<IngredientAlterResponse>> getIngredientSubstitutes(@PathVariable Long id) {
    List<IngredientAlterResponse> responses = alternateIngredientService.getIngredientAndSub(id);
    return new ResponseData<>(200, "Get success", responses);
  }

  @PostMapping("/import")
  public ResponseData<List<RecipeResponse>> importRecipes(
    @RequestBody List<RecipeExtractDTO> listDto) {

    List<RecipeResponse> responses = listDto.stream()
      .map(recipeService::saveExtractResultWithPost)  // gọi lại hàm đã có
      .toList();

    return new ResponseData<>(200, "Import success", responses);
  }

  @PostMapping("/extract")
  public ResponseData<RecipeResponse> extract(@RequestBody RequestUrl requestUrl) {

    RecipeResponse result = recipeService.extractAndCreate(requestUrl.getUrl());

    return new ResponseData<>(200, "Extract success", result);
  }

  @GetMapping("/summary/{id}")
  public ResponseData<RecipeSummaryResponse> getRecipeSummary(@PathVariable Long id) {
    return new ResponseData<>(HttpStatus.OK.value(), "Get success", recipeService.getRecipeSummaryById(id));
  }

  @GetMapping("/search-ids")
  public ResponseData<List<Long>> searchRecipeIds(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String region,
    @RequestParam(required = false) String difficulty
  ){
    List<Long> ids = recipeSearchService.searchRecipeIds(keyword, region, difficulty);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", ids);
  }

  ///có thêm thông tin user so voi recipe response
  @GetMapping("/detail/{id}")
  public ResponseData<RecipeDetailResponse> getRecipeDetail(@PathVariable Long id) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", recipeService.getRecipeDetail(id));
  }

  @DeleteMapping("/{id}")
  public ResponseData<Void> deleteRecipe(@PathVariable Long id) {
    //kiem tra quyen la ADMIN hoac la user da tao
    Long currentUserId = securityUtil.getCurrentUserId();
    boolean isAdmin = securityUtil.hasRole("ADMIN");
    Recipe recipe = recipeService.getEntityRecipe(id);

    if (!isAdmin && !recipe.getAuthorId().equals(currentUserId)) {
      throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
    }

    //xoa recipe
    recipeService.deleteRecipeById(id);
    return new ResponseData<>(HttpStatus.OK.value(), SuccessMessage.DELETE_SUCCESS.getMessage());
  }

  @GetMapping
  public ResponseData<Page<RecipeResponse>> searchService(
    @RequestParam(required = false) String title,
    @RequestParam(required = false) String description,
    @RequestParam(required = false) String region,
    @RequestParam(required = false)Difficulty difficulty,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    var result = recipeService.searchRecipes(title, description, region, difficulty, categoryId, null,pageable);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
  }

  @GetMapping("/{id}")
  public ResponseData<RecipeResponse> getRecipe(@PathVariable Long id) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", recipeService.getRecipeById(id));
  }

  @GetMapping("/public/{id}")
  public ResponseData<RecipeResponse> getRecipeById(@PathVariable Long id) {
    return new ResponseData<>(HttpStatus.OK.value(), "Success", recipeService.getRecipeById(id));
  }

  @PostMapping
  public ResponseData<RecipeResponse> createRecipe(@RequestBody RecipeRequest request) {
    Long authorId = securityUtil.getCurrentUserId();
    var result = recipeService.createRecipe(request, authorId);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
  }

  @PutMapping("/{id}")
  public ResponseData<RecipeResponse> updateRecipe(@PathVariable Long id,
                                     @RequestBody RecipeRequest request) {
    var result = recipeService.updateRecipe(id, request);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);

  }

  @GetMapping("/my")
  public ResponseData<Page<RecipeResponse>> getMyRecipes(
    @RequestParam(required = false) String title,
    @RequestParam(required = false) String description,
    @RequestParam(required = false) String region,
    @RequestParam(required = false) Difficulty difficulty,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
    Long userId = securityUtil.getCurrentUserId();
    log.info("User Id get my recipe: "+userId);

    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    var result = recipeService.searchDraftRecipes(title, description, region, difficulty, categoryId, userId,pageable);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
  }
}