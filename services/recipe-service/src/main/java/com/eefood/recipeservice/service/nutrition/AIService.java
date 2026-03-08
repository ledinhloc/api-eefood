package com.eefood.recipeservice.service.nutrition;

import com.eefood.recipeservice.dto.response.IngredientResponse;
import dev.langchain4j.service.UserMessage;

import java.util.List;

public interface AIService {
    List<IngredientResponse> normalizeIngredient(@UserMessage String userMessage);
}
