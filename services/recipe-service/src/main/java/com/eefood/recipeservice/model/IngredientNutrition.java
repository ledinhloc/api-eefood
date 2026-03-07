package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ingredient_nutrition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class IngredientNutrition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "ingredient_id", nullable = false, unique = true)
    private Ingredient ingredient;

    private Double calories;
    private Double protein;
    private Double fat;
    private Double carb;
    private Double fiber;
    private Double calcium;
    private Double sodium;

    @ManyToOne
    @JoinColumn(name = "source_food_id")
    private FoodNutritionDataset sourceFood;
}
