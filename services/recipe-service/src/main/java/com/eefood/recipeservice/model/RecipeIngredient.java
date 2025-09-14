package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "recipe_ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RecipeIngredient extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipe_id", nullable = false)
  private Recipe recipe;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ingredient_id", nullable = false)
  private Ingredient ingredient;

  @Min(value = 0)
  private Integer quantity;

  @Column(nullable = false)
  private String unit;
}
