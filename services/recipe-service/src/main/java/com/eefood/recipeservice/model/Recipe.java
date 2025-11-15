package com.eefood.recipeservice.model;

import com.eefood.recipeservice.enums.Difficulty;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Recipe extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long authorId;
  @Column(nullable = false)
  private String title;//lọc theo*
  private String description;//lọc theo*
  private String region;//lọc theo*
  private String imageUrl;//*
  private String videoUrl;//*
  private Integer prepTime;//*
  private Integer cookTime;//*

  @Enumerated(EnumType.STRING)
  @Column(length = 7)
  private Difficulty difficulty;//lọc theo*

  @ManyToMany
  @JoinTable(
    name = "recipe_category",
    joinColumns = @JoinColumn(name = "recipe_id"),
    inverseJoinColumns = @JoinColumn(name = "category_id")
  )
  private Set<Category> categories = new HashSet<>();//*

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @SQLRestriction("is_deleted = false")
  private Set<RecipeStep> steps = new HashSet<>();

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @SQLRestriction("is_deleted = false")
  private Set<RecipeIngredient> ingredients = new HashSet<>();

  public void addStep(RecipeStep step) {
    steps.add(step);
    step.setRecipe(this);
  }

  public void removeStep(RecipeStep step) {
    steps.remove(step);
    step.setRecipe(null);
  }

  public void addIngredient(RecipeIngredient ingredient) {
    ingredients.add(ingredient);
    ingredient.setRecipe(this);
  }
}
