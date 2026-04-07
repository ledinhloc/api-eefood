package com.eefood.recipeservice.dto.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientAlterResponse {
    private IngredientResponse ingredient;
    private List<IngredientResponse> substitute;
}
