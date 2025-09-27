package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.request.RecipeRequest;
import com.eefood.recipeservice.dto.response.RecipeResponse;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.enums.Difficulty;
import com.eefood.recipeservice.service.RecipeService;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {
  private final SecurityUtil securityUtil;

  private final RecipeService recipeService;
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
  public RecipeResponse createRecipe(@RequestBody RecipeRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authorId = authentication.getName();
    return recipeService.createRecipe(request, authorId);
  }

  @PutMapping("/{id}")
  public RecipeResponse updateRecipe(@PathVariable Long id,
                                     @RequestBody RecipeRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String authorId = authentication.getName();
    return recipeService.updateRecipe(id, request, authorId);
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