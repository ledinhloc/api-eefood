package com.eefood.recipeservice.repository;

import com.eefood.recipeservice.model.Category;
import com.eefood.recipeservice.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

  Optional<Category> findByDescriptionIgnoreCase(String c);
}
