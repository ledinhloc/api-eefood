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

    @SystemMessage("""
        You are a professional nutritionist. Given the nutrition summary of a recipe, provide:
        1. A short summary (2-3 sentences) in Vietnamese about the nutritional profile.
        2. A health level: one of [EXCELLENT, GOOD, FAIR, POOR].
        3. Practical dietary recommendations in Vietnamese (2-3 sentences).

        RESPOND ONLY IN THIS EXACT JSON FORMAT (no markdown, no backticks):
        {
          "summary": "...",
          "healthLevel": "HEALTHY",
          "recommendation": "..."
        }
        """)
    String analyzeNutrition(@UserMessage String nutritionSummary);
}
