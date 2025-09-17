package com.eefood.recipeservice.service;

import com.eefood.recipeservice.enums.Difficulty;
import com.eefood.recipeservice.model.Category;
import com.eefood.recipeservice.model.Recipe;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class RecipeSpecification {
  private RecipeSpecification() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Specification<Recipe> hasAuthor(Long authorId) {
    if (authorId == null) return null;
    return (root, query, cb) -> cb.equal(root.get("authorId"), authorId);
  }

  public static Specification<Recipe> isNotDeleted(){
    return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
  }

  public static Specification<Recipe> hasTitle(String title){
    if(title == null || title.isEmpty()) return null;
    return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%"+title.toLowerCase()+"%");
  }

  public static Specification<Recipe> hasDescription(String description){
    if(description == null || description.isEmpty()) return null;
    return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%"+description.toLowerCase()+"%");
  }

  public static Specification<Recipe> hasRegion(String region){
    if(region == null || region.isEmpty()) return null;
    return (root, query, cb) -> cb.like(cb.lower(root.get("region")), "%"+region.toLowerCase()+"%");
  }

  public static Specification<Recipe> hasDifficulty(Difficulty difficulty) {
    if (difficulty == null) return null;
    return (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
  }

  public static Specification<Recipe> hasCategoryId(Long categoryId) {
    if(categoryId == null) return null;
    return (root, query, cb) -> {
      Join<Recipe, Category> categories = root.join("categories", JoinType.INNER);
      return cb.equal(categories.get("id"), categoryId);
    };
  }

  public static Specification<Recipe> withFetchJoin(){
    return (root, query, cb) ->{
      if(query.getResultType() != Long.class){
        root.fetch("categories", JoinType.LEFT);
        root.fetch("steps", JoinType.LEFT);
        root.fetch("ingredients", JoinType.LEFT);
      }
      query.distinct(true);
      return cb.conjunction();
    };
  }
}
