package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShoppingItem extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id")
  private Recipe recipe;

  @Column(name = "recipe_title_snapshot")
  private String recipeTitleSnapshot;

  @Column(nullable = false)
  private Integer servings = 1;

  @OneToMany(mappedBy = "shoppingItem", cascade = CascadeType.ALL, orphanRemoval = true)
  @SQLRestriction("is_deleted = false")
  private List<ShoppingIngredient> ingredients = new ArrayList<>();
}
