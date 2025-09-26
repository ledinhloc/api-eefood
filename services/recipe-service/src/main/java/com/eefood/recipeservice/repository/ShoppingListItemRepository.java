package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.ShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingItem, Long> {
  List<ShoppingItem> findAllByUserIdAndIsDeletedFalse(Long userId);
  Optional<ShoppingItem> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);
}
