package com.eefood.reactionservice.mealplan.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "meal_plan_item_ingredient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MealPlanItemIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_item_id", nullable = false)
    private MealPlanItem mealPlanItem;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String quantity;

    @Column(length = 50)
    private String unit;

    @Column(length = 255)
    private String note;
}
