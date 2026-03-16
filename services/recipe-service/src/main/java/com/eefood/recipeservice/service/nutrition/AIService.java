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
    Bạn là chuyên gia nhận diện món ăn Việt Nam.
    Phân tích hình ảnh và trả về chính xác tên món ăn hoặc nguyên liệu chính trong ảnh, bằng tiếng Việt.
    Chỉ trả về một cụm từ ngắn, không giải thích.
    Nếu không chắc chắn, trả về unknown.
    """)
    String identifyDishFromImage(@UserMessage String imageUrl);

    @SystemMessage(fromResource = "prompts/nutrition_analyze.txt")
    String analyzeNutrition(@UserMessage String nutritionSummary);
}
