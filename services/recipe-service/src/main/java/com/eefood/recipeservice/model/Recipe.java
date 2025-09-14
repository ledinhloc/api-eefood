package com.eefood.recipeservice.model;

import com.eefood.recipeservice.enums.Difficulty;
import jakarta.persistence.*;
import java.util.List;
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

  @ElementCollection
  @CollectionTable(
      name = "recipe_dietary_preferences",
      joinColumns = @JoinColumn(name = "recipe_id"))
  @Column(name = "dietary")
  private List<String> dietaryPreferences;
}
