package com.eefood.reactionservice.mealplan.mapper;

import com.eefood.reactionservice.enums.Difficulty;
import com.eefood.reactionservice.mealplan.dto.ai.MealPlanAiCandidate;
import com.eefood.reactionservice.mealplan.dto.response.NutritionAnalysisResponse;
import com.eefood.reactionservice.model.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface MealPlanAiMapper {
    @Mapping(target = "recipeId", source = "post.recipeId")
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "difficulty", expression = "java(toDifficultyName(post.getDifficulty()))")
    @Mapping(target = "ingredientKeywords", expression = "java(toList(post.getRecipeIngredientKeywords()))")
    @Mapping(target = "nutrition", source = "nutrition")
    MealPlanAiCandidate toCandidate(Post post, NutritionAnalysisResponse nutrition);

    default String toDifficultyName(Difficulty difficulty) {
        return difficulty == null ? null : difficulty.name();
    }

    default List<String> toList(Set<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }
}
