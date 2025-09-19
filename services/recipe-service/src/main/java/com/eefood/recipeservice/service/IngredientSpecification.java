package com.eefood.recipeservice.service;

import com.eefood.recipeservice.model.Ingredient;
import org.springframework.data.jpa.domain.Specification;

public class IngredientSpecification {
    private IngredientSpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Specification<Ingredient> hasName(String name) {
        if (name == null || name.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
