package com.eefood.reactionservice.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RecipeIngredientsResponse {
    private List<RecipeIngredient> ingredients;

    @Data
    public static class RecipeIngredient {
        private Double quantity;
        private String unit;
        private Ingredient ingredient;
    }

    @Data
    public static class Ingredient {
        private String name;
    }
}
