package com.eefood.reactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimilarPostResponse {
  private Long postId;
  private Long recipeId;
  private String title;
  private String imageUrl;
  private List<String> matchedIngredients;
}
