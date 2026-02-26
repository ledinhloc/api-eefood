package com.eefood.recipeservice.service;

import com.eefood.recipeservice.dto.response.CategoryResponse;
import com.eefood.recipeservice.mapper.RecipeMapper;
import com.eefood.recipeservice.model.Category;
import com.eefood.recipeservice.repository.CategoryRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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

            // dùng field actual trên entity: "description"
            Expression<String> descLower = cb.lower(root.get("description").as(String.class));
            predicates.add(cb.like(descLower, "%" + name.trim().toLowerCase() + "%"));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return categoryRepository.findAll(spec, pageable).map(recipeMapper::toResponse);
    }

    @Cacheable(value = "categoryListCache")
    public List<CategoryResponse> getListCategories() {
        log.info("Fetching categories from DB...");
        return categoryRepository.findAll().stream().map(recipeMapper::toResponse).collect(Collectors.toList());
    }
}