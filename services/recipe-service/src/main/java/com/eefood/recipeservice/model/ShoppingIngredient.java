package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "shopping_ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShoppingIngredient extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shopping_item_id", nullable = false)
  private ShoppingItem shoppingItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingredient_id")
  private Ingredient ingredient;

  @Column(name = "ingredient_name_snapshot")
  private String ingredientNameSnapshot;

  @Column(nullable = false)
  private Double quantity;

  @Column(nullable = false )
  private String unit;

  @Column(nullable = false)
  private Boolean purchased = false;
}
