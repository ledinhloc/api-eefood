package com.eefood.recipeservice.model;

import com.eefood.recipeservice.enums.Difficulty;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
  private String title;

  private String description;

  private String region;

  private String imageUrl;

  private String videoUrl;

  private Integer prepTime;

  private Integer cookTime;

  @Enumerated(EnumType.STRING)
  @Column(length = 7)
  private Difficulty difficulty;

  @ManyToMany
  @JoinTable(
    name = "recipe_category",
    joinColumns = @JoinColumn(name = "recipe_id"),
    inverseJoinColumns = @JoinColumn(name = "category_id")
  )
  private Set<Category> categories = new HashSet<>();

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<RecipeStep> steps = new HashSet<>();

  @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<RecipeIngredient> ingredients = new HashSet<>();

  public void addStep(RecipeStep step) {
    steps.add(step);
    step.setRecipe(this);
  }

  public void removeStep(RecipeStep step) {
    steps.remove(step);
    step.setRecipe(null);
  }
}
