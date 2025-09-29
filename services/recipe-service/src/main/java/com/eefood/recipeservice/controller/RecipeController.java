package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.response.RecipeResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.enums.Difficulty;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.enums.SuccessMessage;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.service.RecipeService;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import com.eefood.recipeservice.exception.ExceptionUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {
  private final SecurityUtil securityUtil;

  private final RecipeService recipeService;

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
  public RecipeResponse getRecipe(@PathVariable Long id) {
    return recipeService.getRecipeById(id);
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
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authorId = authentication.getName();
    var result = recipeService.updateRecipe(id, request, authorId);
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

    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
    var result = recipeService.searchRecipes(title, description, region, difficulty, categoryId, userId,pageable);
    return new ResponseData<>(HttpStatus.OK.value(), "Success", result);
  }
}