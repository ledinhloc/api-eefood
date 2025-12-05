package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.CategoryResponse;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Category;
import com.eefood.recipeservice.repository.CategoryRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final RecipeMapper recipeMapper;

    public Page<CategoryResponse> getAllCategories(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return categoryRepository.findAll(pageable)
                    .map(recipeMapper::toResponse);
        }

        // Nếu có name -> tìm kiếm theo description (lower-case)
        Specification<Category> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("description")), "%" + name.trim().toLowerCase() + "%"));
            }
            // dùng field actual trên entity: "description"
            Expression<String> descLower = cb.lower(root.get("description").as(String.class));
            predicates.add(cb.like(descLower, "%" + name.trim().toLowerCase() + "%"));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return categoryRepository.findAll(spec, pageable).map(recipeMapper::toResponse);
    }

}