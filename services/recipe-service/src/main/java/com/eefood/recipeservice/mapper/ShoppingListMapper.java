package com.eefood.recipeservice.mapper;

import com.eefood.recipeservice.dto.ShoppingIngredientDto;
import com.eefood.recipeservice.dto.ShoppingItemDto;
import com.eefood.recipeservice.model.ShoppingIngredient;
import com.eefood.recipeservice.model.ShoppingItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShoppingListMapper {

  @Mapping(source = "recipe.id", target = "recipeId")
  @Mapping(target = "recipeTitle", expression = "java(resolveRecipeTitle(entity))")
  ShoppingItemDto toDto(ShoppingItem entity);

  List<ShoppingItemDto> toItemDtoList(List<ShoppingItem> items);

  @Mapping(source = "ingredient.id", target = "ingredientId")
  @Mapping(target = "ingredientName", expression = "java(resolveIngredientName(entity))")
  @Mapping(source = "ingredient.image", target = "image")
  ShoppingIngredientDto toDto(ShoppingIngredient entity);

  List<ShoppingIngredientDto> toIngredientDtoList(List<ShoppingIngredient> items);

  default String resolveRecipeTitle(ShoppingItem entity) {
    if (entity == null) {
      return null;
    }
    if (entity.getRecipeTitleSnapshot() != null && !entity.getRecipeTitleSnapshot().isBlank()) {
      return entity.getRecipeTitleSnapshot();
    }
    return entity.getRecipe() == null ? null : entity.getRecipe().getTitle();
  }

  default String resolveIngredientName(ShoppingIngredient entity) {
    if (entity == null) {
      return null;
    }
    if (entity.getIngredientNameSnapshot() != null && !entity.getIngredientNameSnapshot().isBlank()) {
      return entity.getIngredientNameSnapshot();
    }
    return entity.getIngredient() == null ? null : entity.getIngredient().getName();
  }
}
