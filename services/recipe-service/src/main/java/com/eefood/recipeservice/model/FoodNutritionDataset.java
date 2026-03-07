package com.eefood.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "food_nutrition_dataset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FoodNutritionDataset {
    @Id
    private Long id;

    private String foodNameVi;
    private String foodNameEn;

    private Double energy;
    private Double protein;
    private Double fat;
    private Double carb;
    private Double fiber;
    private Double calcium;
    private Double sodium;
}
