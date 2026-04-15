package com.eefood.recipeservice.dto.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredientAlterResponse {
    private IngredientDetailResponse ingredient;
    private IngredientDetailResponse selectedSubstitute;
    private List<IngredientDetailResponse> substitute;
}
