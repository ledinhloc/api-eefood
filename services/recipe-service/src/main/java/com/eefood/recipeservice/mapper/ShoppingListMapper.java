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
  @Mapping(source = "recipe.title", target = "recipeTitle")
  ShoppingItemDto toDto(ShoppingItem entity);

  List<ShoppingItemDto> toItemDtoList(List<ShoppingItem> items);

  @Mapping(source = "ingredient.id", target = "ingredientId")
  @Mapping(source = "ingredient.name", target = "ingredientName")
  @Mapping(source = "ingredient.image", target = "image")
  ShoppingIngredientDto toDto(ShoppingIngredient entity);

  List<ShoppingIngredientDto> toIngredientDtoList(List<ShoppingIngredient> items);
}
