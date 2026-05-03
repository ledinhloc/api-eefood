package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.dto.RecipeCompareDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeCompareResponse {
    private RecipeCompareDTO recipeA;
    private RecipeCompareDTO recipeB;
}
