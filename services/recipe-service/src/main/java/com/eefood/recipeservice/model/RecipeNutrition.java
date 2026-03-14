package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "recipe_nutrition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RecipeNutrition extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "recipe_id", nullable = false, unique = true)
    private Recipe recipe;

    private Double totalCalories;
    private Double totalProtein;
    private Double totalFat;
    private Double totalCarb;
    private Double totalFiber;
    private Double totalSugar;
    private Double totalCalcium;
    private Double totalSodium;

    private Double healthScore;
}
