package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.ShoppingIngredientDto;
import com.eefood.recipeservice.dto.ShoppingItemDto;
import com.eefood.recipeservice.mapper.ShoppingListMapper;
import com.eefood.recipeservice.model.Recipe;
import com.eefood.recipeservice.model.RecipeIngredient;
import com.eefood.recipeservice.model.ShoppingIngredient;
import com.eefood.recipeservice.model.ShoppingItem;
import com.eefood.recipeservice.repository.RecipeRepository;
import com.eefood.recipeservice.repository.ShoppingIngredientRepository;
import com.eefood.recipeservice.repository.ShoppingItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ShoppingListService {
  private final ShoppingItemRepository itemRepo;
  private final RecipeRepository recipeRepo;
  private final ShoppingIngredientRepository ingredientRepo;
  private final ShoppingListMapper mapper;

  //them mon an vao shopping list
  @Transactional
  public ShoppingItemDto addRecipe(Long userId, Long recipeId, int servings) {
    Recipe recipe = recipeRepo.findById(recipeId)
      .orElseThrow(() -> new RuntimeException("Recipe not found"));

    List<ShoppingItem> items = itemRepo.findAllItems(userId, recipeId);

    if(items.size()>1){
      throw new RuntimeException("Expected 1 ShoppingItem but found more than one item");
    }

    ShoppingItem currentItem;
    // item đã tồn tại
    if (items.size() == 1) {
      currentItem = updateItemWithNewServings(items.get(0), recipe, servings);
    }
    else {
      currentItem = createNewItem(userId, recipe, servings);
    }

    return mapper.toDto(currentItem);
  }

  private ShoppingItem createNewItem(Long userId, Recipe recipe, int servings) {
    ShoppingItem item = ShoppingItem.builder()
      .userId(userId)
      .recipe(recipe)
      .servings(servings)
      .isDeleted(false)
      .ingredients(new ArrayList<>())
      .build();

    recipe.getIngredients().forEach(ri ->
      item.getIngredients().add(
        ShoppingIngredient.builder()
          .shoppingItem(item)
          .ingredient(ri.getIngredient())
          .quantity(ri.getQuantity() * servings)
          .unit(ri.getUnit())
          .purchased(false)
          .isDeleted(false)
          .build()
      )
    );
    return itemRepo.save(item);
  }

  private ShoppingItem updateItemWithNewServings(ShoppingItem item, Recipe recipe, int servings) {
    item.setServings(item.getServings() + servings);

    recipe.getIngredients().forEach(ri -> {
      ShoppingIngredient existing = item.getIngredients().stream()
        .filter(si -> si.getIngredient().getId().equals(ri.getIngredient().getId()))
        .findFirst()
        .orElse(null);

      if (existing != null) {
        existing.setQuantity(existing.getQuantity() + ri.getQuantity() * servings);
      } else {
        item.getIngredients().add(
          ShoppingIngredient.builder()
            .shoppingItem(item)
            .ingredient(ri.getIngredient())
            .quantity(ri.getQuantity() * servings)
            .unit(ri.getUnit())
            .purchased(false)
            .isDeleted(false)
            .build()
        );
      }
    });

    return itemRepo.save(item);
  }

  //Xóa mềm món ăn
  @Transactional
  public void removeRecipe(Long userId, Long itemId){
    ShoppingItem item =
        itemRepo
            .findByIdAndUserIdAndIsDeletedFalse(itemId, userId)
            .orElseThrow(() -> new RuntimeException("Item not found"));
    item.setIsDeleted(true);
    item.getIngredients().forEach(i -> i.setIsDeleted(true));
  }

  // Thay đổi khẩu phần (update số lượng nguyên liệu theo servings mới)
  @Transactional
  public ShoppingItemDto updateServings(Long userId, Long itemId, Integer newServings) {
    ShoppingItem item = itemRepo.findByIdAndUserIdAndIsDeletedFalse(itemId, userId)
      .orElseThrow(() -> new RuntimeException("Item not found"));

    int oldServings = item.getServings();
    item.setServings(newServings);

    for (ShoppingIngredient ing : item.getIngredients()) {
      int basePerServing = ing.getQuantity() / oldServings;
      ing.setQuantity(basePerServing * newServings);
    }

    return mapper.toDto(item);
  }

  //Tích chọn mua nguyen lieu
  @Transactional
  public void togglePurchased(Long userId, Long ingredientId, Boolean purchased){
    ShoppingIngredient ing = ingredientRepo.findById(ingredientId)
      .orElseThrow(() -> new RuntimeException("Ingredient not found"));
    if(!ing.getShoppingItem().getUserId().equals(userId)){
      throw new RuntimeException("Not authorized");
    }
    ing.setPurchased(purchased);
  }

  //get theo recipe
  public List<ShoppingItemDto> getByRecipe(Long userId) {
    return mapper.toItemDtoList(itemRepo.findAllByUserIdAndIsDeletedFalse(userId));
  }

  //get theo nguyen lieu
  public List<ShoppingIngredientDto> getByIngredient(Long userId) {
    List<ShoppingIngredient> ingredients =
      ingredientRepo.findAllByShoppingItemUserIdAndIsDeletedFalse(userId);

    //group theo ingredientId
    Map<Long, ShoppingIngredientDto> grouped = new HashMap<>();
    for(ShoppingIngredient ing: ingredients){
      if(ing.getIsDeleted()) continue;
      Long id = ing.getIngredient().getId();
      grouped.compute(id, (k, v) ->{
        if(v == null){
          return ShoppingIngredientDto.builder()
            .id(ing.getId())
            .ingredientId(id)
            .ingredientName(ing.getIngredient().getName())
            .quantity(ing.getQuantity())
            .unit(ing.getUnit())
            .purchased(ing.getPurchased())
            .build();
        }else {
          v.setQuantity(v.getQuantity() + ing.getQuantity());
          return v;
        }
      });
    }
    return new ArrayList<>(grouped.values());
  }
}
