package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.CategoryResponse;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Category;
import com.eefood.recipeservice.repository.CategoryRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final RecipeMapper recipeMapper;

    public Page<CategoryResponse> getAllCategories(String name, Pageable pageable) {
        Specification<Category> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (name != null && !name.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }

            return predicate;
        };

        return categoryRepository.findAll(spec, pageable).map(recipeMapper::toResponse);
    }
}