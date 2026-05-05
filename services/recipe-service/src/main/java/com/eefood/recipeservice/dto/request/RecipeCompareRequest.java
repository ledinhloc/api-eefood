package com.eefood.recipeservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecipeCompareRequest {
    @NotNull
    private Long recipeIdA;

    @NotNull
    private Long recipeIdB;
}
