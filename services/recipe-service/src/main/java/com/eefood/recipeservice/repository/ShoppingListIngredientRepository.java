package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.ShoppingIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingListIngredientRepository  extends JpaRepository<ShoppingIngredient, Long> {
  List<ShoppingIngredient> findAllByShoppingListItemUserIdAndIsDeletedFalse(Long userId);
}
