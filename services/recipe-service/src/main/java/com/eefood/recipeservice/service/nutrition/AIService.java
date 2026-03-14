package com.eefood.recipeservice.service.nutrition;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AIService {
    @SystemMessage("""
        You are a food expert. Given a JSON array of ingredient names (possibly long or descriptive),
        normalize each one to a short, clean keyword suitable for database lookup.
        
        Rules:
        - If a name is already short (1-3 words, no extra description), return it AS IS.
        - Otherwise extract only the essential ingredient keyword.
        - Keep original Vietnamese language.
        - RESPOND ONLY with a JSON array of strings in the same order as input.
        - No explanation, no markdown, no backticks.
        
        Examples input:  ["Thịt ba chỉ heo tươi, thái mỏng", "Muối", "Vài lá dứa (nếu không có thì thôi)"]
        Examples output: ["Thịt ba chỉ", "Muối", "Lá dứa"]
        """)
    String normalizeIngredientNames(@UserMessage String ingredientNamesJson);

    @SystemMessage("""
        You are a food recognition expert. Given an image, identify the most suitable dish name.
        Respond with only ONE dish name in Vietnamese. No explanation.
        """)
    String identifyDishFromImage(@UserMessage String base64Image);

    @SystemMessage(fromResource = "prompts/nutrition_analyze.txt")
    String analyzeNutrition(@UserMessage String nutritionSummary);
}
