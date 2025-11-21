package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "recipe_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RecipeStep extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recipe_id", nullable = false)
  private Recipe recipe;

  @Min(value = 1)
  @Column(nullable = false)
  private Integer stepNumber;

  private String instruction;

  @ElementCollection
  @CollectionTable(
    name = "recipe_step_images",
    joinColumns = @JoinColumn(name = "recipe_step_id")
  )
  @Column(name = "image_url")
  private List<String> imageUrls;

  @ElementCollection
  @CollectionTable(
    name = "recipe_step_videos",
    joinColumns = @JoinColumn(name = "recipe_step_id")
  )
  @Column(name = "video_url")
  private List<String> videoUrls;

  private Integer stepTime;
}