package com.eefood.recipeservice.model;

import lombok.*;
import co.elastic.clients.elasticsearch._types.mapping.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeDocument {
  private Long id;
  private String title;
  private String description;
  private String region;
  private String difficulty;
}
