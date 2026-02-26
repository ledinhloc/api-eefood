package com.eefood.recipeservice.controller;

import com.eefood.recipeservice.dto.ShoppingIngredientDto;
import com.eefood.recipeservice.dto.ShoppingItemDto;
import com.eefood.recipeservice.dto.response.ResponseData;
import com.eefood.recipeservice.service.ShoppingListService;
import com.eefood.recipeservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shopping")
@RequiredArgsConstructor
public class ShoppingController {
  private final ShoppingListService shoppingListService;
  private final SecurityUtil securityUtil;

  @PostMapping("/add")
  public ResponseData<ShoppingItemDto> addRecipe(
    @RequestParam Long recipeId,
    @RequestParam(defaultValue = "1") Integer servings
  ){
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Success", shoppingListService.addRecipe(userId, recipeId, servings));
  }

  @PostMapping("/chatbot/add")
  public ResponseData<ShoppingItemDto> addRecipe(
          @RequestParam Long recipeId,
          @RequestParam Long userId,
          @RequestParam(defaultValue = "1") Integer servings
  ){
    return new ResponseData<>(HttpStatus.OK.value(), "Success", shoppingListService.addRecipe(userId, recipeId, servings));
  }

  @DeleteMapping("/{itemId}")
  public ResponseData<Void> removeRecipe(
    @PathVariable Long itemId
  ){
    Long userId = securityUtil.getCurrentUserId();
    shoppingListService.removeRecipe(userId, itemId);
    return new ResponseData<>(HttpStatus.OK.value(), "Delete Success");
  }

  @PutMapping("/{itemId}/servings")
  public ResponseData<ShoppingItemDto> updateServings(
    @PathVariable Long itemId,
    @RequestParam Integer servings) {
    Long userId = securityUtil.getCurrentUserId();
    return  new ResponseData<>(HttpStatus.OK.value(), "Success", shoppingListService.updateServings(userId, itemId, servings));
  }

  @PutMapping("/ingredient/purchased")
  public ResponseData<Void> togglePurchased(
    @RequestParam List<Long> ingredientIds,
    @RequestParam Boolean purchased) {
    Long userId = securityUtil.getCurrentUserId();
    shoppingListService.togglePurchased(userId, ingredientIds, purchased);
    return new ResponseData<>(HttpStatus.OK.value(), "Success");
  }

  @GetMapping("/by-recipe")
  public ResponseData<List<ShoppingItemDto>> getByRecipe() {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Success", shoppingListService.getByRecipe(userId));
  }

  @GetMapping("/by-ingredient")
  public ResponseData<List<ShoppingIngredientDto>> getByIngredient() {
    Long userId = securityUtil.getCurrentUserId();
    return new ResponseData<>(HttpStatus.OK.value(), "Success", shoppingListService.getByIngredient(userId));
  }
}
