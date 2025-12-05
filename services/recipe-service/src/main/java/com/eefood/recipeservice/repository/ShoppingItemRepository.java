package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.ShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
  @Query("""
        SELECT s 
        FROM ShoppingItem s 
        JOIN FETCH s.recipe r 
        WHERE s.userId = :userId 
          AND s.isDeleted = false 
        ORDER BY r.title ASC
    """)
  List<ShoppingItem> findAllActiveByUserIdOrderByRecipeTitle(@Param("userId") Long userId);
  Optional<ShoppingItem> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

  @Query(
  """
    select si
    from ShoppingItem si
    where si.userId=:userId
        and si.recipe.id=:recipeId
        and si.isDeleted=false
  """)
  List<ShoppingItem> findAllItems(@Param("userId") Long userId,
                                  @Param("recipeId") Long recipeId);

}
