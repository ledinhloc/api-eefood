package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.ShoppingIngredientDto;
import com.eefood.recipeservice.dto.ShoppingItemDto;
import com.eefood.recipeservice.enums.ErrorMessage;
import com.eefood.recipeservice.exception.ExceptionUtil;
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
    Recipe recipe = recipeRepo.findByIdAndIsDeletedFalse(recipeId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.RECIPE_NOT_FOUND));

    List<ShoppingItem> items = itemRepo.findAllItems(userId, recipeId);

    if(items.size()>1){
      throw ExceptionUtil.badRequest(ErrorMessage.SHOPPING_ITEM_MORE);
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
            .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.SHOPPING_ITEM_NOT_FOUND));
    item.setIsDeleted(true);
    item.getIngredients().forEach(i -> i.setIsDeleted(true));
  }

  // Thay đổi khẩu phần (update số lượng nguyên liệu theo servings mới)
  @Transactional
  public ShoppingItemDto updateServings(Long userId, Long itemId, Integer newServings) {
    ShoppingItem item = itemRepo.findByIdAndUserIdAndIsDeletedFalse(itemId, userId)
      .orElseThrow(() -> ExceptionUtil.notFound(ErrorMessage.SHOPPING_ITEM_NOT_FOUND));

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
  public void togglePurchased(Long userId, List<Long> ingredientIds, Boolean purchased){
    List<ShoppingIngredient> ingredients = ingredientRepo.findAllByIdInAndIsDeletedFalse(ingredientIds);
    if(ingredients.size() != ingredientIds.size()){
      throw ExceptionUtil.notFound(ErrorMessage.INGREDIENT_SHOPPING_NOT_FOUND);
    }

    for(ShoppingIngredient ing : ingredients){
      if(!ing.getShoppingItem().getUserId().equals(userId)){
        throw ExceptionUtil.forbidden(ErrorMessage.ACCESS_DENIED);
      }
      ing.setPurchased(purchased);
    }
    ingredientRepo.saveAll(ingredients);
  }

  //get theo recipe
  public List<ShoppingItemDto> getByRecipe(Long userId) {
    return mapper.toItemDtoList(itemRepo.findAllByUserIdAndIsDeletedFalse(userId));
  }

  //get theo nguyen lieu
  public List<ShoppingIngredientDto> getByIngredient(Long userId) {
    List<ShoppingIngredient> ingredients
      = ingredientRepo.findAllByShoppingItemUserIdAndIsDeletedFalse(userId);

    // key : ingredientId + unit + purchased
    Map<String, ShoppingIngredientDto> grouded = new HashMap<>();
    for(ShoppingIngredient ing : ingredients){
      if(ing.getIsDeleted()) continue;

      Long ingredientId = ing.getIngredient().getId();
      String unit = ing.getUnit() == null ? "" : ing.getUnit().trim().toLowerCase();
      Boolean purchased = ing.getPurchased();

      String key = ingredientId + "||" + unit + "||" + purchased;
      grouded.compute(key, (k, v) ->{
        if(v == null){
          return ShoppingIngredientDto.builder()
            .id(ing.getId())
            .ingredientId(ingredientId)
            .ingredientName(ing.getIngredient().getName())
            .quantity(ing.getQuantity())
            .image(ing.getIngredient().getImage())
            .unit(unit)
            .purchased(purchased)
            .shoppingIngredientIds(new ArrayList<>(List.of(ing.getId())))
            .build();
        }
        else {
          v.setQuantity(v.getQuantity() + ing.getQuantity());
          v.getShoppingIngredientIds().add(ing.getId());
          return v;
        }
      });
    }
    //sort theo ten nguyen lieu
    return grouded.values().stream()
      .sorted(
        Comparator.comparing(ShoppingIngredientDto::getIngredientName, String.CASE_INSENSITIVE_ORDER)
      ).toList();
  }
}
