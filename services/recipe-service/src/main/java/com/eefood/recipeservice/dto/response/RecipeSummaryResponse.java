package com.eefood.recipeservice.dto.response;

import com.eefood.recipeservice.enums.Difficulty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *Dung de tao post
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RecipeSummaryResponse {
  private Long id;
  private String title;
  private String description;
  private String region;
  private String imageUrl;
  private String videoUrl;
  private Integer prepTime;
  private Integer cookTime;
  private Difficulty difficulty;
  private Set<String> recipeCategories = new HashSet<>();
  private Set<String> recipeIngredientKeywords = new HashSet<>();
}